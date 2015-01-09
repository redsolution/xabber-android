package org.jivesoftware.smack.packet;

import java.util.NoSuchElementException;

/**
 * Represents a stream error packet.
 * 
 * @author alexander.ivanov
 * 
 */
public class StreamError {

	public static enum Type {

		/**
		 * The entity has sent XML that cannot be processed.
		 */
		badFormat("bad-format"),

		/**
		 * The entity has sent a namespace prefix that is unsupported, or has
		 * sent no namespace prefix on an element that needs such a prefix.
		 */
		badNamespacePrefix("bad-namespace-prefix"),

		/**
		 * The server either (1) is closing the existing stream for this entity
		 * because a new stream has been initiated that conflicts with the
		 * existing stream, or (2) is refusing a new stream for this entity
		 * because allowing the new stream would conflict with an existing
		 * stream.
		 */
		conflict("conflict"),

		/**
		 * One party is closing the stream because it has reason to believe that
		 * the other party has permanently lost the ability to communicate over
		 * the stream.
		 */
		connectionTimeout("connection-timeout"),

		/**
		 * The value of the 'to' attribute provided in the initial stream header
		 * corresponds to an FQDN that is no longer serviced by the receiving
		 * entity.
		 */
		hostGone("host-gone"),

		/**
		 * The value of the 'to' attribute provided in the initial stream header
		 * does not correspond to an FQDN that is serviced by the receiving
		 * entity.
		 */
		hostUnknown("host-unknown"),

		/**
		 * A stanza sent between two servers lacks a 'to' or 'from' attribute,
		 * the 'from' or 'to' attribute has no value, or the value violates the
		 * rules for XMPP addresses.
		 */
		improperAddressing("improper-addressing"),

		/**
		 * The server has experienced a misconfiguration or other internal error
		 * that prevents it from servicing the stream.
		 */
		internalServerError("internal-server-error"),

		/**
		 * The data provided in a 'from' attribute does not match an authorized
		 * JID or validated domain as negotiated (1) between two servers using
		 * SASL or Server Dialback, or (2) between a client and a server via
		 * SASL authentication and resource binding.
		 */
		invalidFrom("invalid-from"),

		/**
		 * The stream ID or dialback ID is invalid or does not match an ID
		 * previously provided. Has been removed from RFC-6120.
		 */
		invalidId("invalid-id"),

		/**
		 * The stream namespace name is something other than
		 * "http://etherx.jabber.org/streams" or the content namespace declared
		 * as the default namespace is not supported.
		 */
		invalidNamespace("invalid-namespace"),

		/**
		 * The entity has sent invalid XML over the stream to a server that
		 * performs validation.
		 */
		invalidXml("invalid-xml"),

		/**
		 * The entity has attempted to send XML stanzas or other outbound data
		 * before the stream has been authenticated, or otherwise is not
		 * authorized to perform an action related to stream negotiation.
		 */
		notAuthorized("not-authorized"),

		/**
		 * The initiating entity has sent XML that violates the well-formedness
		 * rules of XML or XML‑NAMES.
		 */
		notWellFormed("not-well-formed"),

		/**
		 * The entity has violated some local service policy.
		 */
		policyViolation("policy-violation"),

		/**
		 * The server is unable to properly connect to a remote entity that is
		 * needed for authentication or authorization.
		 */
		remoteConnectionFailed("remote-connection-failed"),

		/**
		 * The server is closing the stream because it has new features to
		 * offer.
		 */
		reset("reset"),

		/**
		 * The server lacks the system resources necessary to service the
		 * stream.
		 */
		resourceConstraint("resource-constraint"),

		/**
		 * The entity has attempted to send restricted XML features such as a
		 * comment, processing instruction, DTD subset, or XML entity reference.
		 */
		restrictedXml("restricted-xml"),

		/**
		 * The server will not provide service to the initiating entity but is
		 * redirecting traffic to another host under the administrative control
		 * of the same service provider.
		 */
		seeOtherHost("see-other-host"),

		/**
		 * The server is being shut down and all active streams are being
		 * closed.
		 */
		systemShutdown("system-shutdown"),

		/**
		 * The error condition is not one of those defined by the other
		 * conditions in this list.
		 */
		undefinedCondition("undefined-condition"),

		/**
		 * The initiating entity has encoded the stream in an encoding that is
		 * not supported by the server or has otherwise improperly encoded the
		 * stream.
		 */
		unsupportedEncoding("unsupported-encoding"),

		/**
		 * The receiving entity has advertised a mandatory-to-negotiate stream
		 * feature that the initiating entity does not support, and has offered
		 * no other mandatory-to-negotiate feature alongside the unsupported
		 * feature.
		 */
		unsupportedFeature("unsupported-feature"),

		/**
		 * The initiating entity has sent a first-level child of the stream that
		 * is not supported by the server, either because the receiving entity
		 * does not understand the namespace or because the receiving entity
		 * does not understand the element name for the applicable namespace.
		 */
		unsupportedStanzaType("unsupported-stanza-type"),

		/**
		 * The 'version' attribute provided by the initiating entity in the
		 * stream header specifies a version of XMPP that is not supported by
		 * the server.
		 */
		unsupportedVersion("unsupported-version");

		String value;

		private Type(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

		public static Type fromString(String value)
				throws NoSuchElementException {
			for (Type type : values())
				if (type.value.equals(value))
					return type;
			throw new NoSuchElementException();
		}
	}

	public static final String ELEMENT_NAME = "stream:error";

	public static final String TYPE_NAMESPACE = "urn:ietf:params:xml:ns:xmpp-streams";

	/**
	 * Error type.
	 */
	private Type type;

	/**
	 * Body of the error type element.
	 */
	private String body;

	/**
	 * @return the error type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @param type
	 *            the error type to set
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * @return the error body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * @param body
	 *            the error body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<");
		builder.append(ELEMENT_NAME);
		builder.append(">");
		if (type != null) {
			builder.append("<");
			builder.append(type.toString());
			builder.append(" xmlns='");
			builder.append(TYPE_NAMESPACE);
			builder.append("'");
			if (body == null)
				builder.append("/>");
			else {
				builder.append(">");
				builder.append(body);
				builder.append("</");
				builder.append(type.toString());
				builder.append(">");
			}
		}
		builder.append("</");
		builder.append(ELEMENT_NAME);
		builder.append(">");
		return builder.toString();
	}

}
