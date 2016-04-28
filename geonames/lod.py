# -*- coding: utf8 -*-
from xml.sax import make_parser, ContentHandler
from xml.sax.handler import feature_namespaces
import urllib, json, hashlib
#import geonames.adapters.search


# This is a SAX handler for loading the BGBM catalog file and extract locality, coordinates, country code
# For each record found that satsifies certain criteria, findGeoNames is called
class catalogueHandler(ContentHandler):

    def __init__(self):
        self.inItem = 0
        self.inTitle = 0
        self.inLocality = 0
        self.inLatitude = 0
        self.inLongitude = 0
        self.inCountry = 0

    def startElement(self, name, attrs):
        if name == "rdf:li":
            self.inItem = 1
            self.title = ""
            self.uri = ""
            self.locality = ""
            self.longitude = ""
            self.latitude = ""
            self.country = ""

        if self.inItem:
            if name == "rdf:Description":
                self.uri = attrs["rdf:about"]
            elif name == "dc:title":
                self.inTitle = 1
            elif name == "dwc:Locality":
                self.inLocality = 1
            elif name == "dwc:decimalLongitude":
                self.inLongitude = 1
            elif name == "dwc:decimalLatitude":
                self.inLatitude = 1
            elif name == "dwc:countryCode":
                self.inCountry = 1

    def characters(self, ch):
        if self.inTitle:
            self.title += ch
        elif self.inLocality:
            self.locality += ch
        elif self.inLongitude:
            self.longitude += ch
        elif self.inLatitude:
            self.latitude += ch
        elif self.inCountry:
            self.country += ch

    def endElement(self, name):
        if name == 'rdf:li':
            self.inItem = 0
            self.locality = self.locality.strip()
            if ":" in self.locality:
                self.locality = self.locality.split(":")[1]
            if "," in self.locality:
                self.locality = self.locality.split(",")[0]
            self.country = self.country.strip()[:2]
            if self.country and not " " in self.locality:
                findGeoNames(self.uri, self.title, self.country, self.locality, self.longitude, self.latitude)
        elif name == "dc:title":
            self.inTitle = 0
        elif name == "dwc:Locality":
            self.inLocality = 0
        elif name == "dwc:decimalLongitude":
            self.inLongitude = 0
        elif name == "dwc:decimalLatitude":
            self.inLatitude = 0
        elif name == "dwc:countryCode":
            self.inCountry = 0


# Looks up the locality in geonames and retrieves the coordinates;
# A bounding box is constructed from them, which is used for a polygon search on the Naturalis API
# Number of records found is sent to stdout
def findGeoNames(uri, title, country, locality, longitude, latitude):
    # print uri, title, country, locality, longitude, latitude

    # This would be the approach using the geonames client library (uncomment import first)
    # gn = geonames.adapters.search.Search("jholetschek")
    # result = gn.query(locality).country(country).max_rows(100).execute()
    # for id_, name in result.get_flat_results():
    #    (do something meaningful here)

    # First, get geonames
    print "\nLooking up %s (%s) on geonames..." % (locality, country)
    latmin, latmax, lonmin, lonmax = 90.0, -90.0, 180.0, -180.0
    params = urllib.urlencode({'name_equals': locality, 'country': country, 'maxRows': 1000, 'username': '<insertusernamehere>', 'type': 'json', 'style': 'short', 'fuzzy': 0})
    r = urllib.urlopen("http://api.geonames.org/search?" + params)
    results = json.loads(r.read())
    cnt = 0
    for name in results['geonames']:
        cnt += 1
        lat = float(name['lat'])
        lon = float(name['lng'])
        if lat < latmin:
            latmin = lat
        if lat > latmax:
            latmax = lat
        if lon < lonmin:
            lonmin = lon
        if lon > lonmax:
            lonmax = lon
        #print "   ", name['name'], "http://geonames.org/%s" % name['geonameId'], name['lat'], name['lng']

    # Then, use the bounding box to retrieve data from Naturalis (but only if the bounding box hasn't been used before)
    # geoshape = urllib.urlencode({"type": "Polygon", "coordinates": [[[lonmin, latmax], [lonmax, latmax], [lonmax, latmin], [lonmin, latmin], [lonmin, latmax]]]})
    geoshape = urllib.quote('{"type":"Polygon","coordinates":[[[%5f, %5f], [%5f, %5f], [%5f, %5f], [%5f, %5f], [%5f, %5f]]]}' % (lonmin, latmax, lonmax, latmax, lonmax, latmin, lonmin, latmin, lonmin, latmax))
    md5 = hashlib.md5(geoshape).hexdigest()
    if md5 in boxes:
        print "Bounding box already retrieved before; skipping"
    else:
        boxes.add(md5)
        print "Retrieving records from Naturalis for bounding box %5f %5f %5f %5f (constructed from %i results)..." % (latmin, latmax, lonmin, lonmax, cnt)
        # params = urllib.urlencode({"_geoShape": geoshape, "_maxResults": 100, '_sort': '_score', '_sortDirection': 'DESC', '_andOr': 'OR'})
        params = '_sort=_score&_sortDirection=DESC&_maxResults=100&_andOr=OR&_geoShape=' + geoshape
        url = 'http://api.biodiversitydata.nl/v0/specimen/name-search?_sort=_score&_sortDirection=DESC&_maxResults=100&_andOr=OR&_geoShape=' + geoshape
        r = json.loads(urllib.urlopen(url).read())
        print ">> %i records found." % r['totalSize']


# Main
if __name__ == "__main__":
    print "BEFORE RUNNING THIS SCRIPT, CREATE AN ACCOUNT ON GEONAMES.\nACTIVATE WEBSERCIVE ACCESS FOR THAT ACCOUNT, THEN ENTER THE USERNAME INTO THE FINDGEONAMES METHOD OF THIS SCRIPT."
    boxes = set()
    parser = make_parser()
    parser.setFeature(feature_namespaces, 0)
    parser.setContentHandler(catalogueHandler())
    parser.parse(open("catalogBGBMsmall.xml", "r"))
