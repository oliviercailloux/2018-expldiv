package io.github.oliviercailloux.expldiv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableList;

class ReadJbmc {

	@Test
	void testLowLevel() throws Exception {
		final Document doc;
		try (InputStream inputStream = ReadJbmc.class.getResourceAsStream("Assignment with 0 not envying 1.xml")) {
			doc = new UFReader().open(inputStream);
		}
		final CProverReader reader = new CProverReader(doc);
		assertEquals(1, reader.getTraceNodes().getLength());
		final NodeList assignmentNodes = reader.getAssignmentNodesToLhs("app");
		assertEquals(2, assignmentNodes.getLength());
		final Node appNode = assignmentNodes.item(1);
		assertEquals("&dynamic_object14", reader.getAssignmentValue(appNode));
		assertEquals("2", reader.getUniqueAssignedValue("dynamic_object15.length"));
		assertEquals("&((void *)dynamic_16_array[0L])", reader.getUniqueAssignedValue("dynamic_object15.data"));
		assertEquals("(void *)&dynamic_object17", reader.getUniqueAssignedValue("(void *)dynamic_16_array[0L]"));
		assertEquals("(void *)&dynamic_object19", reader.getUniqueAssignedValue("(void *)dynamic_16_array[1L]"));
		assertEquals(ImmutableList.of(0, 0, 0), reader.getIntArray("dynamic_20_array", 3));

	}

	@Test
	void testHighLevel() throws Exception {
		final Document doc;
		try (InputStream inputStream = ReadJbmc.class.getResourceAsStream("Assignment with 0 not envying 1.xml")) {
			doc = new UFReader().open(inputStream);
		}
		final CProverReader reader = new CProverReader(doc);

		assertEquals("&dynamic_object14", reader.getSecondAndLastAssignedValue("app"));
		assertEquals("&dynamic_object15", reader.getSecondAndLastAssignedValue("dynamic_object14.utilityFunctions"));
		final ImmutableList<String> referenceArrayObject = reader.getReferenceArrayObject("dynamic_object15");
		assertEquals(ImmutableList.of("(void *)&dynamic_object17", "(void *)&dynamic_object19"), referenceArrayObject);
		assertEquals(ImmutableList.of("dynamic_object17", "dynamic_object19"),
				reader.getDereferenced(referenceArrayObject));
		assertEquals(ImmutableList.of(3, 2, 0), reader.getIntArrayObject("dynamic_object17"));
		assertEquals(ImmutableList.of(0, 0, 0), reader.getIntArrayObject("dynamic_object19"));
	}

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(ReadJbmc.class);
}
