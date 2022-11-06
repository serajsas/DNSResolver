package ca.ubc.cs317.dnslookup;
/**
 * This class represents an exception when the response is missed or the query id does not match the
 * id of the response we retrieved
 */
public class MissedResponseException extends Exception{
	public MissedResponseException() {
		super("Transaction ID does not match");
	}
}
