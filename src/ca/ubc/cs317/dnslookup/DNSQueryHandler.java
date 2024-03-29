package ca.ubc.cs317.dnslookup;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;

public class DNSQueryHandler {

	private static final int DEFAULT_DNS_PORT = 53;
	private static DatagramSocket socket;
	public static boolean verboseTracing = false;

	public static final Random random = new Random();

	/**
	 * Sets up the socket and set the timeout to 5 seconds
	 *
	 * @throws SocketException if the socket could not be opened, or if there was an
	 *                         error with the underlying protocol
	 */
	public static void openSocket() throws SocketException {
		socket = new DatagramSocket();
		socket.setSoTimeout(5000);
	}

	/**
	 * Closes the socket
	 */
	public static void closeSocket() {
		socket.close();
	}

	/**
	 * Set verboseTracing to tracing
	 */
	public static void setVerboseTracing(boolean tracing) {
		verboseTracing = tracing;
	}

	/**
	 * Builds the query, sends it to the server, and returns the response.
	 *
	 * @param message Byte array used to store the query to DNS servers.
	 * @param server  The IP address of the server to which the query is being sent.
	 * @param node    Host and record type to be used for search.
	 * @return A DNSServerResponse Object containing the response buffer and the transaction ID.
	 * @throws IOException if an IO Exception occurs
	 */
	public static DNSServerResponse buildAndSendQuery(byte[] message, InetAddress server, DNSNode node) throws IOException {
		// upper-bound is exclusive, so I added a one to account for 65536
		DNSLookupService.dnsDataHolder.transactionID = random.nextInt(65537);
		return buildAndSendQueryWithID(message, server, node, DNSLookupService.dnsDataHolder);
	}

	/**
	 * Builds the query with a specific ID, sends it to the server, and returns the response.
	 *
	 * This function is mainly used in case of a thrown exception. Another attempt is made with
	 * the same query id but to a different server
	 *
	 * @param message Byte array used to store the query to DNS servers.
	 * @param server The IP address of the server to which the query is being sent.
	 * @param node  Host and record type to be used for search.
	 * @param dnsDataHolder Keeps transaction ID and nameservers for the last query
	 * @return DNSServerResponse
	 * @throws IOException if an IO Exception occurs
	 */
	public static DNSServerResponse buildAndSendQueryWithID(byte[] message, InetAddress server, DNSNode node, DNSDataHolder dnsDataHolder) throws IOException {
		if (verboseTracing) {
			System.out.println();
			System.out.println();
		}
		if (verboseTracing) {
			System.out.println("Query ID     " + dnsDataHolder.transactionID + " " + node.getHostName() + "  " + node.getType() + " --> " + server.getHostAddress());
		}
		message = getMessageQuery(node, dnsDataHolder.transactionID);
		return sendQuery(message, server, dnsDataHolder.transactionID);
	}

	/**
	 * This function just sends the query using socket
	 *
	 * @param message Byte array used to store the query to DNS servers.
	 * @param server The IP address of the server to which the query is being sent
	 * @param transactionID The id of the query
	 * @return DNSServerResponse
	 * @throws IOException if an IO Exception occurs
	 */
	public static DNSServerResponse sendQuery(byte[] message, InetAddress server, int transactionID) throws IOException {
		DatagramPacket sentPacket = new DatagramPacket(message, message.length, server, DEFAULT_DNS_PORT);
		socket.send(sentPacket);
		byte[] responseBuffer = new byte[1024];
		DatagramPacket receivedPacket = new DatagramPacket(responseBuffer, responseBuffer.length);
		socket.receive(receivedPacket);
		return new DNSServerResponse(ByteBuffer.wrap(responseBuffer), transactionID);
	}

	/**
	 *
	 * @param node DNSNode
	 * @param transactionID The id of the query
	 * @return byte[] that contains the message of the query
	 * @throws IOException if an IO Exception occurs
	 */
	public static byte[] getMessageQuery(DNSNode node, int transactionID) throws IOException {
		DNSHeader dnsHeader = new DNSHeader(transactionID, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0001, 0x0000, 0x0000, 0x0000);
		DNSQuery dnsQuery = new DNSQuery(node.getHostName(), node.getType().getCode(), 0x0001);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(stream);
		dnsHeader.encode(outputStream);
		dnsQuery.encode(outputStream);
		return stream.toByteArray();
	}

	/**
	 * Decodes the DNS server response and caches it.
	 *
	 * @param transactionID  Transaction ID of the current communication with the DNS server
	 * @param responseBuffer DNS server's response
	 * @param cache          To store the decoded server's response
	 * @return A set of resource records corresponding to the name servers of the response.
	 */
	public static Set<ResourceRecord> decodeAndCacheResponse(int transactionID, ByteBuffer responseBuffer, DNSCache cache) throws Exception {
		byte[] responseArray = responseBuffer.array();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(responseArray);
		DataInputStream dataInputStream = new DataInputStream(inputStream);
		DNSResponse dnsResponse = new DNSResponse();
		Set<ResourceRecord> result = dnsResponse.decode(transactionID, dataInputStream, responseArray, cache);
		DNSLookupService.dnsDataHolder.nameservers = dnsResponse.dnsrData.nameServers.stream().toList();
		return result;
	}

	/**
	 * Formats and prints record details (for when trace is on)
	 *
	 * @param record The record to be printed
	 * @param rtype  The type of the record to be printed
	 */
	public static void verbosePrintResourceRecord(ResourceRecord record, int rtype) {
		if (verboseTracing)
			System.out.format("       %-30s %-10d %-4s %s\n", record.getHostName(), record.getTTL(), record.getType() == RecordType.OTHER ? rtype : record.getType(), record.getTextResult());
	}
}

