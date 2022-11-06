package ca.ubc.cs317.dnslookup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This class represent a DNS Query
 */
public class DNSQuery {
	public String QNAME;
	public int QTYPE, QCLASS;

	public DNSQuery() {
	}

	public DNSQuery(String QNAME, int QTYPE, int QCLASS) {
		this.QNAME = QNAME;
		this.QTYPE = QTYPE;
		this.QCLASS = QCLASS;
	}

	/**
	 * Decode DNS Query response
	 * @param inputStream DataInputStream
	 * @param responseBuffer byte[]
	 * @param qdcount qdcount
	 * @throws IOException IOException
	 */
	public void decode(DataInputStream inputStream, byte[] responseBuffer, int qdcount) throws IOException {
		// For this assignment the question count is not more than 1
		if (qdcount > 0) {
			QNAME = NameResolver.getName(inputStream, responseBuffer);
			QNAME = QNAME.substring(0, QNAME.length() - 1);
			QTYPE = inputStream.readUnsignedShort();
			QCLASS = inputStream.readUnsignedShort();
		}
	}

	/**
	 * Encode DNS Query
	 * @param stream DataOutputStream
	 * @throws IOException IOException
	 */
	public void encode(DataOutputStream stream) throws IOException {
		String[] domainNames = QNAME.split("\\.");
		for (String domainName : domainNames) {
			stream.writeByte(domainName.length());
			stream.writeBytes(domainName);
		}
		// Indicate the end of Qname
		stream.writeByte(0x0);

		stream.writeShort(QTYPE);
		stream.writeShort(QCLASS);
	}
}
