package ca.ubc.cs317.dnslookup;

import java.io.DataInputStream;
import java.io.IOException;

public class DNSAnswer {
	public String NAME;
	public int TYPE, CLASS, TTL, RDLENGTH;
	public byte[] RDATA;

	public DNSAnswer() {

	}

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
