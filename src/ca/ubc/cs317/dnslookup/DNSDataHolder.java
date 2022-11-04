package ca.ubc.cs317.dnslookup;

import java.util.ArrayList;
import java.util.List;

public class DNSDataHolder {
	public int transactionID;
	public List<ResourceRecord> nameservers;

	public DNSDataHolder(int transactionID) {
		this.transactionID = transactionID;
		this.nameservers = new ArrayList<>();
	}
}
