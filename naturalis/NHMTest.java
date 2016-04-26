package nl.naturalis.nda.elasticsearch.dao.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.naturalis.nda.domain.SourceSystem;
import nl.naturalis.nda.domain.Specimen;

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
			specimen.setUnitID((String) data.get("occurrenceID"));
			specimens.add(specimen);
			break;
		}
		Debug.print("/home/ayco/test.txt", BeanPrinter.toString(specimens));

	}
}
