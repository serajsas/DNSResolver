package ca.ubc.cs317.dnslookup;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * This class is responsible for resolving a name from the response
 */
public class NameResolver {
	/**
	 * This function resolves the name in a response, it handles different cases when there is a pointer/label
	 *
	 * @param inputStream DataInputStream
	 * @param responseBuffer byte[]
	 * @return String that represents the name
	 * @throws IOException if an IO Exception occurs
	 */
	public static String getName(DataInputStream inputStream, byte[] responseBuffer) throws IOException {
		StringBuilder sb = new StringBuilder();
		inputStream.mark(1);
		int length = inputStream.readUnsignedByte();
		while (length != 0) {
			if ((length & 0b11000000) != 0) {
				// Reset the buffer to re-read the byte
				inputStream.reset();
				short offset = (short) (inputStream.readUnsignedShort() & 0x3FFF);
				ByteArrayInputStream ptrInputStream = new ByteArrayInputStream(responseBuffer, offset,
						responseBuffer.length - offset);
				DataInputStream dataInputStream = new DataInputStream(ptrInputStream);
				sb.append(getName(dataInputStream, responseBuffer));
				break;
			} else {
				byte[] bytes = new byte[length];
				inputStream.read(bytes, 0, length);
				sb.append(new String(bytes, StandardCharsets.UTF_8));
				sb.append(".");
				inputStream.mark(1);
				length = inputStream.readUnsignedByte();
			}
		}
		return sb.toString();
	}
}
