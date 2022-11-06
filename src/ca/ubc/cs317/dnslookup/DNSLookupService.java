package ca.ubc.cs317.dnslookup;

import java.io.Console;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

public class DNSLookupService {

	private static boolean p1Flag = false; // isolating part 1
	private static final int MAX_INDIRECTION_LEVEL = 10;
	private static final int QUERY_RESEND_MAX_ATTEMPTS = 10;
	private static InetAddress rootServer;
	private static DNSCache cache = DNSCache.getInstance();
	public static DNSDataHolder dnsDataHolder = new DNSDataHolder(0);

	/**
	 * Main function, called when program is first invoked.
	 *
	 * @param args list of arguments specified in the command line.
	 */
	public static void main(String[] args) {
		if (args.length == 2 && args[1].equals("-p1")) {
			p1Flag = true;
		} else if (args.length != 1) {
			System.err.println("Invalid call. Usage:");
			System.err.println("\tjava -jar DNSLookupService.jar rootServer");
			System.err.println("where rootServer is the IP address (in dotted form) of the root DNS server to start the search at.");
			System.exit(1);
		}

		try {
			rootServer = InetAddress.getByName(args[0]);
			System.out.println("Root DNS server is: " + rootServer.getHostAddress());
		} catch (UnknownHostException e) {
			System.err.println("Invalid root server (" + e.getMessage() + ").");
			System.exit(1);
		}

		try {
			DNSQueryHandler.openSocket();
		} catch (SocketException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		Scanner in = new Scanner(System.in);
		Console console = System.console();
		do {
			// Use console if one is available, or standard input if not.
			String commandLine;
			if (console != null) {
				System.out.print("DNSLOOKUP> ");
				commandLine = console.readLine();
			} else
				try {
					commandLine = in.nextLine();
				} catch (NoSuchElementException ex) {
					break;
				}
			// If reached end-of-file, leave
			if (commandLine == null)
				break;

			// Ignore leading/trailing spaces and anything beyond a comment character
			commandLine = commandLine.trim().split("#", 2)[0];

			// If no command shown, skip to next command
			if (commandLine.trim().isEmpty())
				continue;

			String[] commandArgs = commandLine.split(" ");

			if (commandArgs[0].equalsIgnoreCase("quit") || commandArgs[0].equalsIgnoreCase("exit"))
				break;
			else if (commandArgs[0].equalsIgnoreCase("server")) {
				// SERVER: Change root nameserver
				if (commandArgs.length == 2) {
					try {
						rootServer = InetAddress.getByName(commandArgs[1]);
						System.out.println("Root DNS server is now: " + rootServer.getHostAddress());
					} catch (UnknownHostException e) {
						System.out.println("Invalid root server (" + e.getMessage() + ").");
					}
				} else {
					System.out.println("Invalid call. Format:\n\tserver IP");
				}
			} else if (commandArgs[0].equalsIgnoreCase("trace")) {
				// TRACE: Turn trace setting on or off
				if (commandArgs.length == 2) {
					boolean verboseTracing = false;
					if (commandArgs[1].equalsIgnoreCase("on")) {
						verboseTracing = true;
						DNSQueryHandler.setVerboseTracing(true);
					} else if (commandArgs[1].equalsIgnoreCase("off")) {
						DNSQueryHandler.setVerboseTracing(false);
					} else {
						System.err.println("Invalid call. Format:\n\ttrace on|off");
						continue;
					}
					System.out.println("Verbose tracing is now: " + (verboseTracing ? "ON" : "OFF"));
				} else {
					System.err.println("Invalid call. Format:\n\ttrace on|off");
				}
			} else if (commandArgs[0].equalsIgnoreCase("lookup") || commandArgs[0].equalsIgnoreCase("l")) {
				// LOOKUP: Find and print all results associated to a name.
				RecordType type;
				if (commandArgs.length == 2)
					type = RecordType.A;
				else if (commandArgs.length == 3)
					try {
						type = RecordType.valueOf(commandArgs[2].toUpperCase());
					} catch (IllegalArgumentException ex) {
						System.err.println("Invalid query type. Must be one of:\n\tA, AAAA, NS, MX, CNAME");
						continue;
					}
				else {
					System.err.println("Invalid call. Format:\n\tlookup hostName [type]");
					continue;
				}
				findAndPrintResults(commandArgs[1], type);
			} else if (commandArgs[0].equalsIgnoreCase("dump")) {
				// DUMP: Print all results still cached
				cache.forEachNode(DNSLookupService::printResults);
			} else {
				System.err.println("Invalid command. Valid commands are:");
				System.err.println("\tlookup fqdn [type]");
				System.err.println("\ttrace on|off");
				System.err.println("\tserver IP");
				System.err.println("\tdump");
				System.err.println("\tquit");
			}

		} while (true);

		DNSQueryHandler.closeSocket();
		System.out.println("Goodbye!");
	}

	/**
	 * Finds all results for a host name and type and prints them on the standard output.
	 *
	 * @param hostName Fully qualified domain name of the host being searched.
	 * @param type     Record type for search.
	 */
	private static void findAndPrintResults(String hostName, RecordType type) {
		DNSNode node = new DNSNode(hostName, type);
		printResults(node, getResults(node, 0));
	}

	/**
	 * Finds all the results for a specific node.
	 *
	 * @param node             Host and record type to be used for search.
	 * @param indirectionLevel Control to limit the number of recursive calls due to CNAME redirection.
	 *                         The initial call should be made with 0 (zero), while recursive calls for
	 *                         regarding CNAME results should increment this value by 1. Once this value
	 *                         reaches MAX_INDIRECTION_LEVEL, the function prints an error message and
	 *                         returns an empty set.
	 * @return A set of resource records corresponding to the specific query requested.
	 */
	private static Set<ResourceRecord> getResults(DNSNode node, int indirectionLevel) {
		if (indirectionLevel >= MAX_INDIRECTION_LEVEL) {
			System.err.println("Maximum number of indirection levels reached.");
			return Collections.emptySet();
		}

		Set<ResourceRecord> resourceRecords;
		DNSNode cnameNode = getCnameResourceRecords(node, 0);
		if (cnameNode != null && !cnameNode.equals(node)) {
			resourceRecords = resolveDNS(cnameNode);
			if (!resourceRecords.isEmpty()) {
				ResourceRecord cnameRecord = (ResourceRecord) resourceRecords.toArray()[0];
				node = new DNSNode(cnameRecord.getHostName(), node.getType());
			}
		} else {
			resourceRecords = resolveDNS(node);
		}
		if (resourceRecords.isEmpty()) {
			return getResults(node, indirectionLevel + 1);
		}
		return cache.getCachedResults(node);
	}

	/**
	 * Attempts to resolve CNAME in the response
	 *
	 * @param node             Host and record type to be used for search.
	 * @param indirectionLevel Control to limit the number of recursive calls due to CNAME redirection.
	 *                         The initial call should be made with 0 (zero), while recursive calls for
	 *                         regarding CNAME results should increment this value by 1. Once this value
	 *                         reaches MAX_INDIRECTION_LEVEL, the function prints an error message and
	 *                         returns an empty set.
	 * @return DNSNode
	 */
	private static DNSNode getCnameResourceRecords(DNSNode node, int indirectionLevel) {
		if (indirectionLevel >= MAX_INDIRECTION_LEVEL) {
			System.err.println("Maximum number of indirection levels reached.");
			return null;
		}
		DNSNode cnameServerToUse = new DNSNode(node.getHostName(), RecordType.CNAME);
		if (cache.getCachedResults(cnameServerToUse).isEmpty()) {
			return node;
		}
		ResourceRecord cnameServerIPRecord = (ResourceRecord) cache.getCachedResults(cnameServerToUse).toArray()[0];
		DNSNode cnameNode = new DNSNode(cnameServerIPRecord.getTextResult(), node.getType());
		if (!cache.getCachedResults(cnameNode).isEmpty()) {
			return cnameNode;
		}
		return getCnameResourceRecords(cnameNode, indirectionLevel + 1);

	}

	/**
	 * Resolves all levels until no more levels can be discovered
	 *
	 * @param node Host and record type to be used for search.
	 * @return Set<ResourceRecord>
	 */
	private static Set<ResourceRecord> resolveDNS(DNSNode node) {
		if (cache.getCachedResults(node).isEmpty()) {
			retrieveResultsFromServer(node, rootServer);
		}
		return cache.getCachedResults(node);
	}

	/**
	 * Retrieves DNS results from a specified DNS server. Queries are sent in iterative mode,
	 * and the query is repeated with a new server if the provided one is non-authoritative.
	 * Results are stored in the cache.
	 *
	 * @param node   Host name and record type to be used for the query.
	 * @param server Address of the server to be used for the query.
	 */
	private static void retrieveResultsFromServer(DNSNode node, InetAddress server) {
		byte[] message = new byte[512]; // query is no longer than 512 bytes
		DNSServerResponse serverResponse = null;
		try {
			// Note that a resent query will have the same query ID.
			serverResponse = DNSQueryHandler.buildAndSendQuery(message, server, node);
			Set<ResourceRecord> nameservers = DNSQueryHandler.decodeAndCacheResponse(serverResponse.getTransactionID(), serverResponse.getResponse(), cache);
			if (nameservers == null)
				nameservers = Collections.emptySet();

			queryNextLevel(node, nameservers);

		} catch (MissedResponseException | FlagException | IOException ignored) {
			if (DNSQueryHandler.verboseTracing) {
				System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(), node.getType(), -1, "0.0.0.0");
			}
			reattemptSendAndDecode(message, node, 0);
		} catch (NullPointerException ignored) {
			if (DNSQueryHandler.verboseTracing) {
				System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(), node.getType(), -1, "0.0.0.0");
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Finds the IP of a random nameserver that has been discovered.
	 *
	 * @return List<ResourceRecord>
	 */
	private static List<ResourceRecord> resolveNameServerIntoIP() {
		int randomServerIndex = DNSQueryHandler.random.nextInt(dnsDataHolder.nameservers.size());
		DNSNode nameServerToUseDnsNode = new DNSNode(dnsDataHolder.nameservers.get(randomServerIndex).getTextResult(), RecordType.A);
		return resolveDNS(nameServerToUseDnsNode).stream().toList();
	}

	/**
	 * Re-attempt to send the query that failed, but to a different server each time until QUERY_RESEND_MAX_ATTEMPTS
	 * is reached.
	 *
	 * @param message  Message required to construct the query
	 * @param node     Host name and record type to be used for the query.
	 * @param attempts Usually starts from 0, and attempts to discover the next layer, QUERY_RESEND_MAX_ATTEMPTS times
	 */
	private static void reattemptSendAndDecode(byte[] message, DNSNode node, int attempts) {
		if (attempts >= QUERY_RESEND_MAX_ATTEMPTS) {
			return;
		}
		try {
			List<ResourceRecord> records = resolveNameServerIntoIP();
			int randomServerIndex = DNSQueryHandler.random.nextInt(records.size());
			DNSServerResponse serverResponse = DNSQueryHandler.buildAndSendQueryWithID(message, records.get(randomServerIndex).getInetResult(), node, dnsDataHolder);
			Set<ResourceRecord> nameservers = DNSQueryHandler.decodeAndCacheResponse(serverResponse.getTransactionID(), serverResponse.getResponse(), cache);
			queryNextLevel(node, nameservers);
		} catch (Exception e) {
			if (DNSQueryHandler.verboseTracing) {
				System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(), node.getType(), -1, "0.0.0.0");
			}
			reattemptSendAndDecode(message, node, attempts + 1);
		}
	}


	/**
	 * Query the next level DNS Server, if necessary
	 *
	 * @param node        Host name and record type of the query.
	 * @param nameservers List of name servers returned from the previous level to query the next level.
	 */
	private static void queryNextLevel(DNSNode node, Set<ResourceRecord> nameservers) {
		if (cache.getCachedResults(node).isEmpty() && !nameservers.isEmpty()) {
			List<ResourceRecord> records = resolveNameServerIntoIP();
			if (records.isEmpty()) {
				// No more levels to discover
				return;
			}
			// Query the next level DNS Server
			int resourceRecordIndex = DNSQueryHandler.random.nextInt(records.size());
			ResourceRecord nameServerIPRecord = records.get(resourceRecordIndex);
			retrieveResultsFromServer(node, nameServerIPRecord.getInetResult());
		}
	}

	/**
	 * Prints the result of a DNS query.
	 *
	 * @param node    Host name and record type used for the query.
	 * @param results Set of results to be printed for the node.
	 */
	private static void printResults(DNSNode node, Set<ResourceRecord> results) {
		if (results.isEmpty())
			System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(), node.getType(), -1, "0.0.0.0");
		for (ResourceRecord record : results) {
			System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(), node.getType(), record.getTTL(), record.getTextResult());
		}
	}
}
