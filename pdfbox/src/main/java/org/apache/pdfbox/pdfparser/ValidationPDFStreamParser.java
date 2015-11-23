package org.apache.pdfbox.pdfparser;

import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.cos.validation.ValidationCOSString;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Timur Kamalov
 */
public class ValidationPDFStreamParser extends PDFStreamParser {

	public ValidationPDFStreamParser(InputStream inputStream) throws IOException {
		super(new InputStreamSource(inputStream));
	}

	@Override
	public COSString initializeCOSString(byte[] bytes) {
		return new ValidationCOSString(bytes);
	}

	@Override
	protected COSString parseCOSHexString() throws IOException {
		return validationParseCOSHexString();
	}

	// during this parsing we count hex characters and check for invalid ones
	// pdf/a-1b specification, clause 6.1.6
	private ValidationCOSString validationParseCOSHexString() throws IOException {
		Boolean isHexSymbols = Boolean.TRUE;
		Long hexCount = Long.valueOf(0);

		final StringBuilder sBuf = new StringBuilder();
		while (true) {
			int c = seqSource.read();
			if (isHexDigit((char) c)) {
				sBuf.append((char) c);
				hexCount++;
			} else if (c == '>') {
				break;
			} else if (c < 0) {
				throw new IOException("Missing closing bracket for hex string. Reached EOS.");
			} else if (isWhitespace(c)) {
				continue;
			} else {
				isHexSymbols = Boolean.FALSE;
				hexCount++;
			}
		}
		ValidationCOSString result = ValidationCOSString.parseHex(sBuf.toString());
		result.setHexCount(hexCount);
		result.setContainsOnlyHex(isHexSymbols);

		return result;
	}

}
