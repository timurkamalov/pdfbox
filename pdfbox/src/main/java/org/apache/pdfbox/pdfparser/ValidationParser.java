package org.apache.pdfbox.pdfparser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSObjectKey;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.cos.validation.ValidationCOSDocument;
import org.apache.pdfbox.cos.validation.ValidationCOSString;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.util.Charsets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Timur Kamalov
 */
public class ValidationParser extends PDFParser {

	private static final Log LOG = LogFactory.getLog(ValidationParser.class);

	public ValidationParser(RandomAccessRead source) throws IOException {
		super(source);
	}

	@Override
	protected COSString parseCOSHexString() throws IOException {
		return validationParseCOSHexString();
	}

	@Override
	protected void checkXrefOffsets() throws IOException {
		strictCheckXrefOffsets();
	}

	@Override
	protected boolean checkObjectHeader(String objectString) throws IOException {
		return isObjHeader(objectString);
	}

	@Override
	protected int lastIndexOf(final char[] pattern, final byte[] buf, final int endOff) {
		int offset = super.lastIndexOf(pattern, buf, endOff);
		if (offset > 0 && Arrays.equals(pattern, EOF_MARKER)) {
			// this is the offset of the last %%EOF sequence.
			// nothing should be present after this sequence.
			int tmpOffset = offset + pattern.length;
			if (tmpOffset != buf.length) {
				// EOL is authorized
				final int postEOFDataSize = buf.length - tmpOffset;
				if ((buf.length - tmpOffset) > 2
						|| (buf.length - tmpOffset == 2 && (buf[tmpOffset] != 13 || buf[tmpOffset + 1] != 10))
						|| (buf.length - tmpOffset == 1 && (buf[tmpOffset] != 13 && buf[tmpOffset] != 10))) {
					((ValidationCOSDocument) document).setPostEOFDataSize(postEOFDataSize);
				}
			}
		}
		return offset;
	}

	/**
	 * Parse all objects of document according to xref table
	 */
	protected void parseSuspensionObjects() {
		for (COSObjectKey key : document.getXrefTable().keySet()) {
			try {
				source.seek(document.getXrefTable().get(key));
				COSObject suspensionObject = document.getObjectFromPool(key);
				parseObjectDynamically(suspensionObject, false);
			} catch (IOException e) {
				LOG.error(e);
			}
		}
	}

	// during this parsing we count hex characters and check for invalid ones
	// pdf/a-1b specification, clause 6.1.6
	private ValidationCOSString validationParseCOSHexString() throws IOException {
		Boolean isHexSymbols = Boolean.TRUE;
		Long hexCount = Long.valueOf(0);

		final StringBuilder sBuf = new StringBuilder();
		while (true) {
			int c = source.read();
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
		result.setIsHexSymbols(isHexSymbols);

		return result;
	}

	/**
	 * Check the XRef table by dereferencing all objects and fixing the offset if necessary.
	 * Doesn't store objects with corrupted offset
	 *
	 * @throws IOException if something went wrong.
	 */
	private void strictCheckXrefOffsets() throws IOException {
		Map<COSObjectKey, Long> xrefOffset = xrefTrailerResolver.getXrefTable();
		if (xrefOffset != null) {
			List<COSObjectKey> objectsToRemove = new ArrayList<COSObjectKey>();
			for (Map.Entry<COSObjectKey, Long> objectEntry : xrefOffset.entrySet()) {
				COSObjectKey objectKey = objectEntry.getKey();
				Long objectOffset = objectEntry.getValue();
				// a negative offset number represents a object number itself
				// see type 2 entry in xref stream
				if (objectOffset != null && objectOffset >= 0
						&& !checkObjectKeys(objectKey, objectOffset)) {
					objectsToRemove.add(objectKey);
					LOG.warn("Object " + objectKey + " has invalid offset");
				}
			}
			for (COSObjectKey key : objectsToRemove) {
				xrefOffset.remove(key);
			}
		}
	}

	private boolean isObjHeader(String expectedObjHeader) throws IOException {
		long objN = readObjectNumber();
		int genN = readGenerationNumber();
		//to ensure that we have "obj" keyword
		readExpectedString(OBJ_MARKER, true);
		String actualObjHeader = createObjectString(objN, genN);
		return actualObjHeader.equals(expectedObjHeader);
	}

	/**
	 * This will read bytes until the first end of line marker occurs, but EOL markers
	 * will not skipped.
	 *
	 * @return The characters between the current position and the end of the line.
	 * @throws IOException If there is an error reading from the stream.
	 */
	private String readLineWithoutWhitespacesSkip() throws IOException {
		if (source.isEOF()) {
			throw new IOException("Error: End-of-File, expected line");
		}

		StringBuilder buffer = new StringBuilder(11);

		int c;
		while ((c = source.read()) != -1) {
			// CR and LF are valid EOLs
			if (isEOL(c) || c == 32) {
				source.rewind(1);
				break;
			}
			buffer.append((char) c);
		}
		return buffer.toString();
	}

}
