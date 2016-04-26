package nl.naturalis.nda.elasticsearch.dao.dao;

import static nl.naturalis.nda.domain.TaxonomicRank.FAMILY;
import static nl.naturalis.nda.domain.TaxonomicRank.GENUS;
import static nl.naturalis.nda.domain.TaxonomicRank.KINGDOM;
import static nl.naturalis.nda.domain.TaxonomicRank.ORDER;
import static nl.naturalis.nda.domain.TaxonomicRank.PHYLUM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.naturalis.nda.domain.DefaultClassification;
import nl.naturalis.nda.domain.Monomial;
import nl.naturalis.nda.domain.ScientificName;
import nl.naturalis.nda.domain.SourceSystem;
import nl.naturalis.nda.domain.Specimen;
import nl.naturalis.nda.domain.SpecimenIdentification;

import org.domainobject.util.debug.BeanPrinter;
import org.domainobject.util.debug.Debug;
import org.domainobject.util.http.SimpleHttpGet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests making a call to NHM and converting the response to the NBA data model. This code
 * has also been inserted into the Specimen DAO in the LOD-pilot git branch.
 * 
 * @author ayco
 *
 */
public class NHMTest {

	public static void main(String[] args) throws Exception
	{
		SimpleHttpGet httpGet = new SimpleHttpGet();
		httpGet.setBaseUrl("http://data.nhm.ac.uk/api/action/datastore_search?resource_id=05ff2255-c38a-40c9-b657-4ccb55ab2feb&q=egg&limit=3");
		httpGet.execute();
		byte[] response = httpGet.getResponseBody();
		ObjectMapper om = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
		HashMap<String, Object> map = om.readValue(response, typeRef);
		Map result = (Map) map.get("result");
		Debug.print("/home/ayco/input.txt", BeanPrinter.toString(result));
		List records = (List) result.get("records");
		Iterator iterator = records.iterator();
		List<Specimen> specimens = new ArrayList<>();
		while (iterator.hasNext()) {
			Map nhmData = (Map) iterator.next();
			Specimen specimen = new Specimen();
			specimen.setSourceSystem(SourceSystem.NHM);
			specimen.setUnitID(get(nhmData, "occurrenceID"));
			specimen.setRecordBasis(get(nhmData, "basisOfRecord"));
			ScientificName sn = new ScientificName();
			sn.setFullScientificName(get(nhmData, "scientificName"));
			sn.setAuthorshipVerbatim(get(nhmData, "scientificNameAuthorship"));
			SpecimenIdentification identification = new SpecimenIdentification();
			identification.setScientificName(sn);
			List<Monomial> systemClassification = new ArrayList<>();
			systemClassification.add(new Monomial(KINGDOM, get(nhmData, "kingdom")));
			systemClassification.add(new Monomial(PHYLUM, get(nhmData, "phylum")));
			systemClassification.add(new Monomial(ORDER, get(nhmData, "order")));
			systemClassification.add(new Monomial(FAMILY, get(nhmData, "family")));
			systemClassification.add(new Monomial(GENUS, get(nhmData, "genus")));
			List<SpecimenIdentification> identifications = new ArrayList<>();
			identifications.add(identification);
			specimen.setIdentifications(identifications);
			specimens.add(specimen);
			DefaultClassification dc = DefaultClassification
					.fromSystemClassification(systemClassification);
			identification.setDefaultClassification(dc);
		}
		Debug.print("/home/ayco/output.txt", BeanPrinter.toString(specimens));

	}

	private static String get(Map data, String s)
	{
		return (String) data.get(s);
	}
}
