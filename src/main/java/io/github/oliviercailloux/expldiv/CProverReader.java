package io.github.oliviercailloux.expldiv;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class CProverReader {
	final Document source;

	public CProverReader(Document source) {
		this.source = requireNonNull(source);
		final Element docE = source.getDocumentElement();
		LOGGER.debug("Main tag name: {}.", docE.getTagName());
		xPath = XPathFactory.newInstance().newXPath();
		getTraceNodes();
	}

	public ImmutableList<Integer> getIntArray(String fullLhs, int length) {
		/** Retrives an lhs named *array* of type int []. */
		final String typeRegex = "int \\[.+\\]";
		final ImmutableList<String> strings = getArray(fullLhs, length, typeRegex);
		final Builder<Integer> builder = ImmutableList.builderWithExpectedSize(length);
		for (String valueStr : strings) {
			final Integer valueInt = Integer.valueOf(valueStr);
			builder.add(valueInt);
		}
		return builder.build();
	}

	public ImmutableList<String> getArray(String fullLhs, int length, String typeRegex) {
		checkArgument(fullLhs.contains("array"));
		checkArgument(length >= 0);
		final Builder<String> builderStr = ImmutableList.builderWithExpectedSize(length);
		for (int i = 0; i < length; ++i) {
			final Node assignment = getUniqueAssignementNodeToLhs(fullLhs + "[" + i + "L]");
			final String type = getType(assignment);
			checkArgument(type.matches(typeRegex), type);
			final String valueStr = getAssignmentValue(assignment);
			builderStr.add(valueStr);
		}
		final ImmutableList<String> strings = builderStr.build();
		return strings;
	}

	public ImmutableList<String> getReferenceArrayObject(String fullLhs) {
		/** Of type struct java::array[reference] (having complex types as data). */
		checkArgument(fullLhs.contains("dynamic_object"));
		final String typeRegex = "struct java::array\\[reference\\].*";
		final Node lengthAssignment = getUniqueAssignementNodeToLhs(fullLhs + ".length");
		{
			final String type = getType(lengthAssignment);
			checkArgument(type.matches(typeRegex), type);
		}
		final String lengthStr = getAssignmentValue(lengthAssignment);
		final int length = Integer.parseInt(lengthStr);
		final Node dataAssignment = getUniqueAssignementNodeToLhs(fullLhs + ".data");
		{
			final String type = getType(dataAssignment);
			checkArgument(type.matches(typeRegex));
		}
		final String data = getAssignmentValue(dataAssignment);
		final Pattern pattern = Pattern.compile("&\\((\\(void \\*\\)dynamic_[0-9]+_array)\\[0L\\]\\)");
		final Matcher matcher = pattern.matcher(data);
		checkArgument(matcher.matches());
		final String array = matcher.group(1);
		return getArray(array, length, "struct java::array\\[int\\].*");
	}

	public NodeList getAssignmentNodesToBaseName(String baseName) {
		final String correctExpression = "/cprover/result/goto_trace/assignment[@base_name='" + baseName + "']";
		return getNodesNoExc(correctExpression);
	}

	private NodeList getNodesNoExc(String correctExpression) {
		try {
			return getNodes(correctExpression);
		} catch (XPathExpressionException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(CProverReader.class);
	private final XPath xPath;

	public NodeList getNodes(String expression) throws XPathExpressionException {
		return (NodeList) xPath.evaluate(expression, source, XPathConstants.NODESET);
	}

	public NodeList getTraceNodes() {
		final String correctExpression = "/cprover/result/goto_trace";
		return getNodesNoExc(correctExpression);
	}

	public String getType(Node assignment) {
		final Node node = getUniqueChildNamed(assignment, "type");
		return getTextFromSimpleNode(node);
	}

	private String getTextFromSimpleNode(Node node) {
		final NodeList children = node.getChildNodes();
		assert children.getLength() == 1;
		final Node content = children.item(0);
		assert content.getNodeType() == Node.TEXT_NODE;
		return content.getNodeValue();
	}

	private Node getUniqueChildNamed(Node parent, String childNodeName) {
		final NodeList children = parent.getChildNodes();
		Node found = null;
		for (int i = 0; i < children.getLength(); ++i) {
			final Node child = children.item(i);
			final String name = child.getNodeName();
			if (name.equals(childNodeName)) {
				if (found != null) {
					throw new IllegalArgumentException();
				}
				found = child;
			}
		}
		if (found == null) {
			throw new IllegalArgumentException();
		}
		return found;
	}

	public String getUniqueAssignedValue(String fullLhs) {
		final Node assignment = getUniqueAssignementNodeToLhs(fullLhs);
		return getAssignmentValue(assignment);
	}

	public Node getUniqueAssignementNodeToLhs(String fullLhs) {
		final NodeList nodes = getAssignmentNodesToLhs(fullLhs);
		if (nodes.getLength() != 1) {
			throw new IllegalArgumentException(fullLhs);
		}
		final Node assignment = nodes.item(0);
		return assignment;
	}

	public NodeList getAssignmentNodesToLhs(String fullLhs) {
		return getNodesNoExc("/cprover/result/goto_trace/assignment[full_lhs='" + fullLhs + "']");
	}

	public String getSecondAndLastAssignedValue(String fullLhs) {
		final NodeList nodes = getAssignmentNodesToLhs(fullLhs);
		if (nodes.getLength() != 2) {
			throw new IllegalArgumentException();
		}
		final Node assignment = nodes.item(1);
		return getAssignmentValue(assignment);
	}

	public String getAssignmentValue(Node assignment) {
		final Node node = getUniqueChildNamed(assignment, "full_lhs_value");
		return getTextFromSimpleNode(node);
	}

	public ImmutableList<Integer> getIntArrayObject(String fullLhs) {
		/** Of type struct java::array[int] (having an IntArray as data). */
		checkArgument(fullLhs.contains("dynamic_object"));
		final Node lengthAssignment = getUniqueAssignementNodeToLhs(fullLhs + ".length");
		{
			final String type = getType(lengthAssignment);
			checkArgument(type.contains("struct java::array[int]"));
		}
		final String lengthStr = getAssignmentValue(lengthAssignment);
		final int length = Integer.parseInt(lengthStr);
		final Node dataAssignment = getUniqueAssignementNodeToLhs(fullLhs + ".data");
		{
			final String type = getType(dataAssignment);
			checkArgument(type.contains("struct java::array[int]"));
		}
		final String data = getAssignmentValue(dataAssignment);
		return getIntArray(data, length);
	}

	public ImmutableList<String> getDereferenced(ImmutableList<String> referenceArrayObject) {
		final Pattern pattern = Pattern.compile("\\(void \\*\\)&(dynamic_object[0-9]+)");
		return referenceArrayObject.stream().map((s) -> {
			final Matcher matcher = pattern.matcher(s);
			checkArgument(matcher.matches());
			return matcher.group(1);
		}).collect(ImmutableList.toImmutableList());
	}
}
