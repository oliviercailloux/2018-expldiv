package io.github.oliviercailloux.expldiv;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSException;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class UFReader {
	private ImmutableMap<Integer, ImmutableList<Integer>> utilityFunctions;
	private Document doc;
	private final Pattern deref;

	public UFReader() {
		deref = Pattern.compile("&(.*)");
	}

	public void read() {
		final CProverReader reader = new CProverReader(doc);
		final String andAppObj = reader.getSecondAndLastAssignedValue("app");
		final String appObj = deref(andAppObj);
		final String andUfObj = reader.getSecondAndLastAssignedValue(appObj + ".utilityFunctions");
		final String ufObj = deref(andUfObj);
		final ImmutableList<String> referencedUfs = reader.getReferenceArrayObject(ufObj);
		final ImmutableList<String> objectsUfs = reader.getDereferenced(referencedUfs);
		final Builder<Integer, ImmutableList<Integer>> builder = ImmutableMap
				.builderWithExpectedSize(objectsUfs.size());
		int user = 0;
		for (String objectUf : objectsUfs) {
			final ImmutableList<Integer> uf = reader.getIntArrayObject(objectUf);
			builder.put(user, uf);
			++user;
		}
		utilityFunctions = builder.build();
	}

	public static void main(String[] args) throws Exception {
		final UFReader reader = new UFReader();
		reader.open(Paths.get("out.xml"));
		reader.read();
		LOGGER.info("UFs: {}.", reader.getUtilityFunctions());
	}

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(UFReader.class);

	private void open(Path path) throws IOException {
		try (InputStream inputStream = Files.newInputStream(path)) {
			open(inputStream);
		}
	}

	private String deref(String object) {
		final Matcher matcher = deref.matcher(object);
		checkArgument(matcher.matches());
		final String dereferenced = matcher.group(1);
		return dereferenced;
	}

	public Document open(InputStream inputStream) throws IOException {
		DOMImplementationRegistry registry;
		try {
			registry = DOMImplementationRegistry.newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
			throw new IllegalStateException(e);
		}
		final DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
		final LSParser builder = impl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
		final LSInput lsInput = impl.createLSInput();
		lsInput.setByteStream(inputStream);
		try {
			doc = builder.parse(lsInput);
		} catch (LSException e) {
			throw new IOException(e);
		}
		return doc;
	}

	public ImmutableMap<Integer, ImmutableList<Integer>> getUtilityFunctions() {
		return utilityFunctions;
	}

	public Document getDoc() {
		return doc;
	}
}
