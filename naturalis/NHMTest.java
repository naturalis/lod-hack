package nl.naturalis.nda.elasticsearch.dao.dao;

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
import nl.naturalis.nda.domain.TaxonomicRank;

import org.domainobject.util.FileUtil;
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
		SimpleHttpGet nhm = new SimpleHttpGet();
		nhm.setBaseUrl("http://data.nhm.ac.uk/api/action/datastore_search?resource_id=05ff2255-c38a-40c9-b657-4ccb55ab2feb&q=egg&limit=3");
		nhm.execute();
		byte[] response = nhm.getResponseBody();
		ObjectMapper om = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
		HashMap<String, Object> map = om.readValue(response, typeRef);
		Map result = (Map) map.get("result");
		Debug.print("/home/ayco/input.txt", BeanPrinter.toString(result));
		List records = (List) result.get("records");
		Iterator iterator = records.iterator();
		List<Specimen> specimens = new ArrayList<>();
		while (iterator.hasNext()) {
			Map data = (Map) iterator.next();
			Specimen specimen = new Specimen();
			specimen.setSourceSystem(SourceSystem.NHM);
			specimen.setUnitID(get(data, "occurrenceID"));
			specimen.setRecordBasis(get(data, "basisOfRecord"));
			ScientificName sn = new ScientificName();
			sn.setFullScientificName(get(data, "scientificName"));
			sn.setAuthorshipVerbatim(get(data, "scientificNameAuthorship"));
			SpecimenIdentification identification = new SpecimenIdentification();
			identification.setScientificName(sn);
			List<Monomial> systemClassification = new ArrayList<>();
			systemClassification.add(new Monomial(TaxonomicRank.KINGDOM, get(data, "kingdom")));
			systemClassification.add(new Monomial(TaxonomicRank.PHYLUM, get(data, "phylum")));
			systemClassification.add(new Monomial(TaxonomicRank.ORDER, get(data, "order")));
			systemClassification.add(new Monomial(TaxonomicRank.FAMILY, get(data, "family")));
			systemClassification.add(new Monomial(TaxonomicRank.GENUS, get(data, "genus")));
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
