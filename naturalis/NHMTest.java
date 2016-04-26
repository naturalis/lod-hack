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

import org.domainobject.util.debug.BeanPrinter;
import org.domainobject.util.debug.Debug;
import org.domainobject.util.http.SimpleHttpGet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NHMTest {

	public static void main(String[] args) throws Exception
	{
		SimpleHttpGet nhm = new SimpleHttpGet();
		nhm.setBaseUrl("http://data.nhm.ac.uk/api/action/datastore_search?resource_id=05ff2255-c38a-40c9-b657-4ccb55ab2feb&q=egg");
		nhm.execute();
		byte[] res = nhm.getResponseBody();
		ObjectMapper om = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
		HashMap<String, Object> map = om.readValue(res, typeRef);
		Map result = (Map) map.get("result");
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
			String s = "scientificName";
			sn.setFullScientificName(get(data, "scientificName"));
			sn.setAuthorshipVerbatim(get(data, "scientificNameAuthorship"));
			SpecimenIdentification si = new SpecimenIdentification();
			si.setScientificName(sn);
			List<Monomial> systemClassification = new ArrayList<>();
			systemClassification.add(new Monomial(TaxonomicRank.KINGDOM, get(data, "kingdom")));
			systemClassification.add(new Monomial(TaxonomicRank.PHYLUM, get(data, "phylum")));
			systemClassification.add(new Monomial(TaxonomicRank.ORDER, get(data, "order")));
			systemClassification.add(new Monomial(TaxonomicRank.FAMILY, get(data, "family")));
			systemClassification.add(new Monomial(TaxonomicRank.GENUS, get(data, "genus")));
			specimen.addIndentification(si);
			specimens.add(specimen);
			DefaultClassification dc = DefaultClassification
					.fromSystemClassification(systemClassification);
			si.setDefaultClassification(dc);

			break;
		}
		Debug.print("/home/ayco/test.txt", BeanPrinter.toString(specimens));

	}

	private static String get(Map data, String s)
	{
		return (String) data.get(s);
	}
}
