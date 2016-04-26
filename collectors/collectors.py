#!/usr/bin/env python
# encoding: utf-8
"""
Created by Ben Scott on '25/04/2016'.
"""

# import requests


import requests
import dataset
import difflib
import requests_cache
import urllib2

# https://gist.github.com/crstn/4554603


from rdflib import Graph, Literal, BNode, Namespace, RDF, URIRef
from rdflib.namespace import DC, FOAF


collectors = {

    'http://viaf.org/viaf/51750221': {
        'name': 'Aimé Jacques Alexandre Bonpland',
        'search_terms': {
            'nhm': 'Jacques Alexandre Bonpland',
            'naturalis': 'Bonpland, AJA'
        }
    },
    'http://viaf.org/viaf/95193235': {
        'name': 'Humboldt, Alexander von (Friedrich Wilhelm Heinrich Alexander), 1769-1859',
        'search_terms': {
            'nhm': 'Friedrich Wilhelm Heinrich Alexander von Humboldt; b.1769; d.1859; Humb.',
            'naturalis': 'Humboldt, FWHA von'
        }
    },
    'http://viaf.org/viaf/9962177': {
        'name': 'Wallich, Nathaniel, 1786-1854',
        'search_terms': {
            'nhm': 'Nathaniel Wallich; b.1786; d.1854; Wall',
            'naturalis': 'Wallich, N'
        }
    },
    'http://viaf.org/viaf/73914023': {
        'name': 'Adolf Engler',
        'search_terms': {
            'nhm': 'Heinrich Gustav Adolf Engler; b.1844; d.1930; Engl.',
            'naturalis': 'Engler, HGA'
        }
    },
    'http://viaf.org/viaf/88234994': {
        'name': 'Sellow, Friedrich 1789-1831',
        'search_terms': {
            'nhm': 'Friedrich Sellow; b.1789; d.1831; Sellow',
            'naturalis': 'Sellow, F'
        }
    }

}

requests_cache.install_cache('api_cache')

TDWGI = Namespace('http://rs.tdwg.org/ontology/voc/Institution#')
AIISO = Namespace('http://purl.org/vocab/aiiso/schema#')
MADS = Namespace('http://www.loc.gov/mads/rdf/v1#')

VOID = Namespace('http://rdfs.org/ns/void#')

ORG = Namespace('http://www.w3.org/TR/vocab-org/#')
# Darwin core
DWC = Namespace('http://rs.tdwg.org/dwc/terms/')
SDWC = Namespace('http://rs.tdwg.org/dwc/xsd/simpledarwincore#')

class API:

    def get_payload(self, **kwargs):
        return kwargs

    def search(self, **kwargs):

        self.specimens = []
        payload = self.get_payload(**kwargs)
        offset = 0

        while True:
            print('Retrieving %s-%s' % (offset, offset+self.per_page))
            payload[self.offset_key] = offset
            payload[self.per_page_key] = self.per_page
            r = requests.get(self.uri, params=payload)
            # r.raise_for_status()
            result = r.json()
            try:
                self.process_result(result, **kwargs)
            except StopIteration:
                break;
            offset+=self.per_page

        return self.specimens

    def add_specimen(self, uri, scientific_name):
        self.specimens.append((uri, scientific_name))


class NHMDataPortalAPI(API):

    uri = 'http://data.nhm.ac.uk/api/action/datastore_search'
    per_page = 100
    offset_key = 'offset'
    per_page_key = 'limit'

    def process_result(self, result, q, **kwargs):

        if not result['result']['records']:
            raise StopIteration

        for record in result['result']['records']:
            recorded_by = record.get('recordedBy');
            occurrence_id = record.get('occurrenceID');
            uri = 'http://data.nhm.ac.uk/object/%s' % occurrence_id
            if q in recorded_by:
                self.add_specimen(uri, record.get('scientificName'))


class NaturalisAPI(API):

    uri = 'http://api.biodiversitydata.nl/v0/specimen/search'
    per_page = 1000
    offset_key = '_offset'
    per_page_key = '_maxResults'

    def get_payload(self, **kwargs):

        # Service does not handle commas well?
        if ',' in kwargs['_search']:
            kwargs['_search'] = kwargs['_search'].split(',')[0]
        return kwargs

    def process_result(self, result, _search, **kwargs):

        if not 'searchResults' in result:
            raise StopIteration

        skipped_names = set()

        for record in result['searchResults']:
            gathering_event = record['result'].get('gatheringEvent')
            for agent in gathering_event['gatheringAgents']:

                if(record['matchInfo'][0]['path'] != 'gatheringEvent.gatheringPersons.fullName'):
                    continue

                collector = agent['fullName']

                if _search in collector:

                    uri = record['result'].get('unitGUID')

                    # URI with spaces don't resolve
                    if '%20' in uri:
                        continue

                    scientific_name = (record['result']['identifications'][0]['scientificName']['fullScientificName'])
                    self.add_specimen(uri, scientific_name)
                else:
                    skipped_names.add(collector)

        # Debugging
        # print(list(skipped_names))



def g_add_specimen(g, specimen_uri, scientific_name, collector_uri, institution_code):
    specimen_uri_ref = URIRef(specimen_uri + '#object')
    g.add((specimen_uri_ref, RDF.type, DWC.PreservedSpecimen))
    g.add((specimen_uri_ref, DWC.recordedBy, collector_uri))
    g.add((specimen_uri_ref, DWC.scientificName, Literal(scientific_name)))
    g.add((specimen_uri_ref, DWC.institutionCode, Literal(institution_code)))

def main():

    g = Graph()

    for uri, collector in collectors.items():

        collector_uri = URIRef(uri)
        g.add( (collector_uri, RDF.type, FOAF.Person) )
        g.add((collector_uri, FOAF.name, Literal(collector['name'])))

        nhm_api = NHMDataPortalAPI()
        nhm_specimens = nhm_api.search(resource_id='05ff2255-c38a-40c9-b657-4ccb55ab2feb', q=collector['search_terms']['nhm'])
        for uri, scientific_name in nhm_specimens:
            g_add_specimen(g, uri, scientific_name, collector_uri, 'NHMUK')

        naturalis_api = NaturalisAPI()
        naturalis_specimens = naturalis_api.search(_search=collector['search_terms']['naturalis'])
        # print(naturalis_specimens)
        for uri, scientific_name in naturalis_specimens:
            g_add_specimen(g, uri, scientific_name, collector_uri, 'NBC')

        # BGBM have exposed VIAF IDS so much easier to lookup
        viaf_id = uri.split('/')[-1]
        r = requests.get('http://ww2.bgbm.org/rest/herb/viafid/%s' % viaf_id)
        r.raise_for_status()
        result = r.json()

        # FIXME: Are BGBM paged? We should have 2500 records?
        for record in result['DATA']:
            uri = record[0]
            scientific_name = record[1]
            g_add_specimen(g, uri, scientific_name, collector_uri, 'BGBM')

    # Bind a few prefix, namespace pairs for more readable output
    g.bind("dc", DC)
    g.bind("foaf", FOAF)
    g.serialize(destination='collectors.ttl', format='turtle')



if __name__ == "__main__":
    main()