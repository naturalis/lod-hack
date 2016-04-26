package nl.naturalis.nda.elasticsearch.dao.dao;

import static nl.naturalis.nda.domain.TaxonomicRank.FAMILY;
import static nl.naturalis.nda.domain.TaxonomicRank.GENUS;
import static nl.naturalis.nda.domain.TaxonomicRank.SPECIES;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.naturalis.nda.domain.Agent;
import nl.naturalis.nda.domain.DefaultClassification;
import nl.naturalis.nda.domain.GatheringEvent;
import nl.naturalis.nda.domain.Monomial;
import nl.naturalis.nda.domain.Person;
import nl.naturalis.nda.domain.ScientificName;
import nl.naturalis.nda.domain.SourceSystem;
import nl.naturalis.nda.domain.Specimen;
import nl.naturalis.nda.domain.SpecimenIdentification;
import nl.naturalis.nda.search.SearchResultSet;

import org.domainobject.util.debug.BeanPrinter;
import org.domainobject.util.debug.Debug;
import org.domainobject.util.http.SimpleHttpGet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test class calling the BGBM REST service and converting it output to the NBA domain
 * model
 * 
 * @author ayco
 *
 */
public class BGBMTest {

	public static void main(String[] args) throws Exception
	{
		int SCIENTIFICNAME = 11;
		int OBJECTURI = 0;
		int BASEOFRECORDS = 7;
		int CATALOGNUMBER = 9;
		int _FAMILY = 12;
		int _GENUS = 13;
		int SPECIFICEPITHET = 14;
		int LOCALITY = 17;
		int COLLECTOR = 3;
		SearchResultSet<Specimen> searchResult = new SearchResultSet<>();
		SimpleHttpGet httpGet = new SimpleHttpGet();
		httpGet.setBaseUrl("http://ww2.bgbm.org/rest/herb/collector/wallich");
		httpGet.execute();
		byte[] response = httpGet.getResponseBody();
		ObjectMapper om = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
		HashMap<String, Object> map = om.readValue(response, typeRef);
		List<List> records = (List) map.get("DATA");
		for (List record : records) {
			Specimen specimen = new Specimen();
			searchResult.addSearchResult(specimen);
			specimen.setSourceSystem(SourceSystem.BGBM);
			specimen.setSourceSystemId(field(record, CATALOGNUMBER));
			specimen.setUnitID(field(record, CATALOGNUMBER));
			specimen.setRecordURI(URI.create(field(record, OBJECTURI)));
			specimen.setRecordBasis(field(record, BASEOFRECORDS));
			List<SpecimenIdentification> identifications = new ArrayList<>();
			specimen.setIdentifications(identifications);
			SpecimenIdentification identification = new SpecimenIdentification();
			identifications.add(identification);
			ScientificName sn = new ScientificName();
			identification.setScientificName(sn);
			sn.setFullScientificName(field(record, SCIENTIFICNAME));
			List<Monomial> systemClassification = new ArrayList<>();
			identification.setSystemClassification(systemClassification);
			systemClassification.add(new Monomial(FAMILY, field(record, _FAMILY)));
			systemClassification.add(new Monomial(GENUS, field(record, _GENUS)));
			systemClassification.add(new Monomial(SPECIES, field(record, SPECIFICEPITHET)));
			DefaultClassification dc = DefaultClassification
					.fromSystemClassification(systemClassification);
			identification.setDefaultClassification(dc);
			GatheringEvent gathering = new GatheringEvent();
			specimen.setGatheringEvent(gathering);
			gathering.setLocality(field(record, LOCALITY));
			Person collector = new Person();
			collector.setFullName(field(record, COLLECTOR));
			List<Agent> agents = new ArrayList<>();
			agents.add(collector);
			gathering.setGatheringAgents(agents);
		}
		Debug.print("/home/ayco/bgbm-input.txt", BeanPrinter.toString(map));
		Debug.print("/home/ayco/bgbm-output.txt", BeanPrinter.toString(searchResult));
	}

	private static String field(List record, int field)
	{
		return (String) record.get(field);
	}
}
