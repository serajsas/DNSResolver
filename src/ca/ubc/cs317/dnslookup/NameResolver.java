package ca.ubc.cs317.dnslookup;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class NameResolver {
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
