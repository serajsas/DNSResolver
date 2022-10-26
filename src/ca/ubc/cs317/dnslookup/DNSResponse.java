package ca.ubc.cs317.dnslookup;

import java.io.DataInputStream;
import java.util.Set;

public class DNSResponse {
	public DNSHeader dnsHeader;
	public DNSQuery dnsQuery;
	public DNSRData dnsrData;

	public DNSResponse() {
		dnsHeader = new DNSHeader();
		dnsQuery = new DNSQuery();
		dnsrData = new DNSRData();
	}

	public Set<ResourceRecord> decode(int transactionID, DataInputStream dataInputStream, byte[] responseBuffer,
									  DNSCache cache) throws Exception {
		dnsHeader.decode(dataInputStream, transactionID);
		dnsQuery.decode(dataInputStream, responseBuffer, dnsHeader.QDCOUNT);
		dnsrData.decode(dataInputStream, responseBuffer, dnsHeader.ANCOUNT,
				dnsHeader.ARCOUNT, dnsHeader.NSCOUNT, cache);
		if (DNSQueryHandler.verboseTracing) {
			System.out.println("Response ID: Authoritative = " + (dnsHeader.AA != 0));
			System.out.println("  Answers " + "(" + dnsHeader.ANCOUNT + ")");
			for (ResourceRecord r : dnsrData.answers) {
				DNSQueryHandler.verbosePrintResourceRecord(r, r.getType().getCode());
			}
			System.out.println("  Nameservers " + "(" + dnsHeader.NSCOUNT + ")");
			for (ResourceRecord r : dnsrData.nameServers) {
				DNSQueryHandler.verbosePrintResourceRecord(r, r.getType().getCode());
			}
			System.out.println("  Additional Information " + "(" + dnsHeader.ARCOUNT + ")");
			for (ResourceRecord r : dnsrData.additionalInformation) {
				DNSQueryHandler.verbosePrintResourceRecord(r, r.getType().getCode());
			}
		}
		return dnsrData.nameServers;
	}
}