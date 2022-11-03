package ca.ubc.cs317.dnslookup;

public class MissedResponseException extends Exception{
	public MissedResponseException() {
		super("Transaction ID does not match");
	}
}
