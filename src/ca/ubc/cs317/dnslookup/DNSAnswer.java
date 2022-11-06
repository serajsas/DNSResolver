package ca.ubc.cs317.dnslookup;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * A class that represents the DNS answer
 * This class is used to decode the response and form Resource records
 */
public class DNSAnswer {
	public String NAME;
	public int TYPE, CLASS, TTL, RDLENGTH;
	public byte[] RDATA;

	public DNSAnswer() {

	}

	/**
	 * Decodes the components of an answer
	 * @param inputStream DataInputStream
	 * @param responseBuffer byte[]
	 * @throws IOException IOException
	 */
	public void decode(DataInputStream inputStream, byte[] responseBuffer) throws IOException {
		NAME = NameResolver.getName(inputStream, responseBuffer);
		NAME = NAME.substring(0, NAME.length() - 1);
		TYPE = inputStream.readUnsignedShort();
		CLASS = inputStream.readUnsignedShort();
		TTL = inputStream.readInt();
		RDLENGTH = inputStream.readShort();
		RDATA = new byte[RDLENGTH];
		inputStream.read(RDATA, 0, RDLENGTH);
	}
}
