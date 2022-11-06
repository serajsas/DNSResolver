package ca.ubc.cs317.dnslookup;

/**
 * This class represents an exception when the response contains invalid flag
 */
public class FlagException extends Exception{
	public FlagException() {
		super("Flag exception in response");
	}
}
