package ca.ubc.cs317.dnslookup;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.HashSet;
import java.util.Set;

/**
 * This class keeps a set of each answers, nameServers, and additionalInformation
 */
public class DNSRData {
	public Set<ResourceRecord> answers;
	public Set<ResourceRecord> nameServers;
	public Set<ResourceRecord> additionalInformation;

	public DNSRData() {
		answers = new HashSet<>();
		nameServers = new HashSet<>();
		additionalInformation = new HashSet<>();
	}

	/**
	 * Decode the input stream and caches the Resource records
	 *
	 * @param dataInputStream DataInputStream
	 * @param responseBuffer byte[]
	 * @param ANCOUNT Answer Count
	 * @param ARCOUNT Additional Information Count
	 * @param NSCOUNT Name Servers count
	 * @param cache Cache to store the result
	 * @throws IOException if an IO Exception occurs
	 */
	public void decode(DataInputStream dataInputStream, byte[] responseBuffer, int ANCOUNT,
					   int ARCOUNT, int NSCOUNT, DNSCache cache) throws IOException {
		for (int i = 0; i < ANCOUNT + ARCOUNT + NSCOUNT; i++) {
			DNSAnswer dnsAnswer = new DNSAnswer();
			dnsAnswer.decode(dataInputStream, responseBuffer);
			ResourceRecord resourceRecord;
			if (RecordType.getByCode(dnsAnswer.TYPE) == RecordType.A) {
				resourceRecord = new ResourceRecord(dnsAnswer.NAME, RecordType.getByCode(dnsAnswer.TYPE),
						dnsAnswer.TTL, Inet4Address.getByAddress(dnsAnswer.NAME, dnsAnswer.RDATA));
			} else if (RecordType.getByCode(dnsAnswer.TYPE) == RecordType.AAAA) {
				resourceRecord = new ResourceRecord(dnsAnswer.NAME, RecordType.getByCode(dnsAnswer.TYPE),
						dnsAnswer.TTL, Inet6Address.getByAddress(dnsAnswer.NAME, dnsAnswer.RDATA));
			} else if (RecordType.getByCode(dnsAnswer.TYPE) == RecordType.NS
					|| RecordType.getByCode(dnsAnswer.TYPE) == RecordType.CNAME) {
				ByteArrayInputStream NSInputStream = new ByteArrayInputStream(dnsAnswer.RDATA, 0,
						dnsAnswer.RDLENGTH);
				DataInputStream NSDataStream = new DataInputStream(NSInputStream);
				String name = NameResolver.getName(NSDataStream, responseBuffer);
				resourceRecord = new ResourceRecord(dnsAnswer.NAME, RecordType.getByCode(dnsAnswer.TYPE),
						dnsAnswer.TTL, name.substring(0, name.length() - 1));
			} else {
				resourceRecord = new ResourceRecord(dnsAnswer.NAME, RecordType.getByCode(dnsAnswer.TYPE),
						dnsAnswer.TTL, "----");
			}

			if(i < ANCOUNT){
				answers.add(resourceRecord);
			}else if(i < ANCOUNT + NSCOUNT){
				nameServers.add(resourceRecord);
			}else{
				additionalInformation.add(resourceRecord);
			}
			cache.addResult(resourceRecord);
		}
	}
}
