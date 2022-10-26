package ca.ubc.cs317.dnslookup;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DNSHeader {
	public int ID, QR, Opcode, AA, TC, RD, RA, Z, RCODE, QDCOUNT, ANCOUNT, NSCOUNT, ARCOUNT;

	public DNSHeader() {
	}

	public DNSHeader(int ID, int QR, int opcode, int AA, int TC, int RD, int RA, int z,
					 int RCODE, int QDCOUNT, int ANCOUNT, int NSCOUNT, int ARCOUNT) {
		this.ID = ID;
		this.QR = QR;
		this.Opcode = opcode;
		this.AA = AA;
		this.TC = TC;
		this.RD = RD;
		this.RA = RA;
		this.Z = z;
		this.RCODE = RCODE;
		this.QDCOUNT = QDCOUNT;
		this.ANCOUNT = ANCOUNT;
		this.NSCOUNT = NSCOUNT;
		this.ARCOUNT = ARCOUNT;
	}

	public void decode(DataInputStream dataInputStream, int transactionID) throws IOException {
		this.ID = dataInputStream.readUnsignedShort();
		if (ID != transactionID) {
			return;
		}
		int bits = dataInputStream.readUnsignedShort();
		QR = (bits >>> 15) & 0b1;
		Opcode = (bits >>> 11) & 0b1111;
		AA = (bits >>> 10) & 0b1;
		TC = (bits >>> 9) & 0b1;
		RD = (bits >>> 8) & 0b1;
		RA = (bits >>> 7) & 0b1;
		Z = (bits >>> 4) & 0b111;
		RCODE = bits & 0b1111;
		QDCOUNT = dataInputStream.readUnsignedShort();
		ANCOUNT = dataInputStream.readUnsignedShort();
		NSCOUNT = dataInputStream.readUnsignedShort();
		ARCOUNT = dataInputStream.readUnsignedShort();
	}

	public void encode(DataOutputStream outputStream) throws IOException {
		outputStream.writeShort(this.ID);
		outputStream.writeShort((QR << 15) + (Opcode << 11) + (AA << 10) +
				(TC << 9) + (RD << 8) + (RA << 7) + (Z << 4) + RCODE);
		outputStream.writeShort(QDCOUNT);
		outputStream.writeShort(ANCOUNT);
		outputStream.writeShort(NSCOUNT);
		outputStream.writeShort(ARCOUNT);
	}
}
