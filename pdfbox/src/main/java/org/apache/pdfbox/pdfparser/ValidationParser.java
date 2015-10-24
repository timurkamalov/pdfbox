package org.apache.pdfbox.pdfparser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.cos.validation.ValidationCOSDocument;
import org.apache.pdfbox.cos.validation.ValidationCOSObject;
import org.apache.pdfbox.cos.validation.ValidationCOSStream;
import org.apache.pdfbox.cos.validation.ValidationCOSString;
import org.apache.pdfbox.io.RandomAccessRead;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import static org.apache.pdfbox.util.Charsets.ISO_8859_1;

/**
 * @author Timur Kamalov
 */
public class ValidationParser extends PDFParser {

	private static final Log LOG = LogFactory.getLog(ValidationParser.class);

	private static final String PDF_HEADER = "%PDF-";
	private static final String PDF_DEFAULT_VERSION = "1.4";

	private final int LINEARIZATION_SIZE = 1024;

	public ValidationParser(RandomAccessRead source) throws IOException {
		super(source);
	}

	@Override
	protected boolean parsePDFHeader() throws IOException {
		return parseAndValidateHeader(PDF_HEADER, PDF_DEFAULT_VERSION);
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

	@Override
	protected void parseFileObject(Long offsetOrObjstmObNr, final COSObjectKey objKey, final COSObject pdfObject) throws IOException {
		// ---- go to object start
		source.seek(offsetOrObjstmObNr);

		//Check that if offset doesn't point to obj key there is eol character before obj key
		//pdf/a-1b spec, clause 6.1.8
		skipSpaces();
		source.seek(source.getPosition() - 1);
		if (!isEOL()) {
			((ValidationCOSObject) pdfObject).setHeaderOfObjectComplyPDFA(Boolean.FALSE);
		}

		// ---- we must have an indirect object
		final long readObjNr = readObjectNumber();
		if ((source.read() != 32) || skipSpaces() > 0) {
			//check correct spacing (6.1.8 clause)
			((ValidationCOSObject) pdfObject).setHeaderFormatComplyPDFA(Boolean.FALSE);
		}
		final int readObjGen = readGenerationNumber();
		if ((source.read() != 32) || skipSpaces() > 0) {
			//check correct spacing (6.1.8 clause)
			((ValidationCOSObject) pdfObject).setHeaderFormatComplyPDFA(Boolean.FALSE);
		}
		readExpectedString(OBJ_MARKER, false);

		// ---- consistency check
		if ((readObjNr != objKey.getNumber()) || (readObjGen != objKey.getGeneration())) {
			String message = "XREF for " + objKey.getNumber() + ":"
					+ objKey.getGeneration() + " points to wrong object: " + readObjNr
					+ ":" + readObjGen;
			LOG.warn(message);
			pdfObject.setObject(COSNull.NULL);
			return;
		}

		if (!isEOL()) {
			// eol marker shall follow the "obj" keyword
			((ValidationCOSObject) pdfObject).setHeaderOfObjectComplyPDFA(Boolean.FALSE);
		}
		COSBase pb = parseDirObject();

		// eolMarker stores symbol before endobj or stream keyword for pdf/a validation
		int eolMarker = 0;
		skipSpaces();
		source.seek(source.getPosition() - 1);
		eolMarker = source.read();

		String endObjectKey = readString();

		if (endObjectKey.equals(STREAM_STRING)) {
			source.rewind(endObjectKey.getBytes(ISO_8859_1).length);
			if (pb instanceof COSDictionary) {
				COSStream stream = parseCOSStream((COSDictionary) pb);

				if (securityHandler != null) {
					securityHandler.decryptStream(stream, objKey.getNumber(), objKey.getGeneration());
				}
				pb = stream;
			} else {
				// this is not legal
				// the combination of a dict and the stream/endstream
				// forms a complete stream object
				throw new IOException("Stream not preceded by dictionary (offset: "
						+ offsetOrObjstmObNr + ").");
			}
			skipSpaces();
			source.rewind(1);
			eolMarker = source.read();
			endObjectKey = readLineWithoutWhitespacesSkip();

			// we have case with a second 'endstream' before endobj
			if (!endObjectKey.startsWith(ENDOBJ_STRING) && endObjectKey.startsWith(ENDSTREAM_STRING)) {
				endObjectKey = endObjectKey.substring(9).trim();
				if (endObjectKey.length() == 0) {
					skipSpaces();
					endObjectKey = readLineWithoutWhitespacesSkip();
					eolMarker = source.read();
				}
			}
		} else if (securityHandler != null) {
			securityHandler.decrypt(pb, objKey.getNumber(), objKey.getGeneration());
		}

		//pdf/a-1b clause 6.1.8
		if (!isEOL(eolMarker)) {
			((ValidationCOSObject) pdfObject).setEndOfObjectComplyPDFA(Boolean.FALSE);
		}

		pdfObject.setObject(pb);

		if (!endObjectKey.startsWith(ENDOBJ_STRING)) {
			LOG.warn("Object (" + readObjNr + ":" + readObjGen + ") at offset "
					+ offsetOrObjstmObNr + " does not end with 'endobj' but with '"
					+ endObjectKey + "'");
		}

		eolMarker = source.read();
		if (!isEOL(eolMarker)) {
			((ValidationCOSObject) pdfObject).setEndOfObjectComplyPDFA(Boolean.FALSE);
			source.rewind(1);
		}
	}

	@Override
	protected COSStream parseCOSStream(COSDictionary dic) throws IOException {
		ValidationCOSStream stream = (ValidationCOSStream) document.createCOSStream(dic);

		// read 'stream'; this was already tested in parseObjectsDynamically()
		readString();

		// pdf/a-1b specification, clause 6.1.7
		checkStreamSpacings(stream);
		stream.setOriginLength(source.getPosition());

		skipWhiteSpaces();

        /*
		 * This needs to be dic.getItem because when we are parsing, the underlying object might still be null.
         */
		COSNumber streamLengthObj = getLength(dic.getItem(COSName.LENGTH), dic.getCOSName(COSName.TYPE));
		if (streamLengthObj == null) {
			throw new IOException("Missing length for stream.");
		}

		// get output stream to copy data to
		if (streamLengthObj != null && validateStreamLength(streamLengthObj.longValue())) {
			OutputStream out = stream.createRawOutputStream();
			try {
				readValidStream(out, streamLengthObj);
			} finally {
				out.close();
				// restore original (possibly incorrect) length
				stream.setItem(COSName.LENGTH, streamLengthObj);
			}
		} else {
			OutputStream out = stream.createRawOutputStream();
			try {
				readUntilEndStream(new EndstreamOutputStream(out));
			} finally {
				out.close();
				// restore original (possibly incorrect) length
				if (streamLengthObj != null) {
					stream.setItem(COSName.LENGTH, streamLengthObj);
				} else {
					stream.removeItem(COSName.LENGTH);
				}
			}
		}

		// pdf/a-1b specification, clause 6.1.7
		checkEndStreamSpacings(stream, streamLengthObj.longValue());

		String endStream = readString();
		if (!endStream.equals(ENDSTREAM_STRING)) {
			throw new IOException("Error reading stream, expected='endstream' actual='" + endStream + "' at offset " + source.getPosition());
		}

		return stream;
	}

	@Override
	protected boolean parseXrefTable(long startByteOffset) throws IOException {
		if (source.peek() != 'x') {
			return false;
		}
		String xref = readString();
		if (!xref.trim().equals("xref")) {
			return false;
		}

		//check spacings after "xref" keyword
		//pdf/a-1b specification, clause 6.1.4
		int space;
		space = source.read();
		if (space == 0x0D) {
			if (source.peek() == 0x0A) {
				source.read();
			}
			if (!isDigit()) {
				((ValidationCOSDocument) document).setIsXrefEOLMarkersComplyPDFA(Boolean.FALSE);
			}
		} else if (space != 0x0A || !isDigit()) {
			((ValidationCOSDocument) document).setIsXrefEOLMarkersComplyPDFA(Boolean.FALSE);
		}

		// check for trailer after xref
		String str = readString();
		byte[] b = str.getBytes(ISO_8859_1);
		source.rewind(b.length);

		// signal start of new XRef
		xrefTrailerResolver.nextXrefObj(startByteOffset, XrefTrailerResolver.XRefType.TABLE);

		if (str.startsWith("trailer")) {
			LOG.warn("skipping empty xref table");
			return false;
		}

		// Xref tables can have multiple sections. Each starts with a starting object id and a count.
		while (true) {
			// first obj id
			long currObjID = readObjectNumber();

			space = source.read();
			if (space != 0x20 || !isDigit()) {
				((ValidationCOSDocument) document).setIsSubsectionHeaderSpaceSeparated(Boolean.FALSE);
			}

			// the number of objects in the xref table
			long count = readLong();

			skipSpaces();
			for (int i = 0; i < count; i++) {
				if (source.isEOF() || isEndOfName((char) source.peek())) {
					break;
				}
				if (source.peek() == 't') {
					break;
				}
				//Ignore table contents
				String currentLine = readLine();
				String[] splitString = currentLine.split("\\s");
				if (splitString.length < 3) {
					LOG.warn("invalid xref line: " + currentLine);
					break;
				}
				/* This supports the corrupt table as reported in
                 * PDFBOX-474 (XXXX XXX XX n) */
				if (splitString[splitString.length - 1].equals("n")) {
					try {
						int currOffset = Integer.parseInt(splitString[0]);
						int currGenID = Integer.parseInt(splitString[1]);
						COSObjectKey objKey = new COSObjectKey(currObjID, currGenID);
						xrefTrailerResolver.setXRef(objKey, currOffset);
					} catch (NumberFormatException e) {
						throw new IOException(e);
					}
				} else if (!splitString[2].equals("f")) {
					throw new IOException("Corrupt XRefTable Entry - ObjID:" + currObjID);
				}
				currObjID++;
				skipSpaces();
			}
			skipSpaces();
			if (!isDigit()) {
				break;
			}
		}
		return true;
	}


	private boolean parseAndValidateHeader(String headerMarker, String defaultVersion) throws IOException {
		/*
            6.1.2 File header
            The % character of the file header shall occur at byte offset 0 of the file.
            The file header line shall be immediately followed by a comment consisting of a % character followed by at least four characters,
            each of whose encoded byte values shall have a decimal value greater than 127.
        */
		// read first line
		String header = readLine();
		// some pdf-documents are broken and the pdf-version is in one of the following lines
		if (!header.contains(headerMarker)) {
			header = readLine();
			while (!header.contains(headerMarker) && !header.contains(headerMarker.substring(1))) {
				// if a line starts with a digit, it has to be the first one with data in it
				if ((header.length() > 0) && (Character.isDigit(header.charAt(0)))) {
					break;
				}
				header = readLine();
			}
		}

		//sometimes there is some garbage in the header before the header
		//actually starts, so lets try to find the header first.
		int headerStart = header.indexOf(headerMarker);

		final long headerOffset = this.source.getPosition() - header.length() + headerStart;
		((ValidationCOSDocument) document).setHeaderOffset(headerOffset);
		((ValidationCOSDocument) document).setHeader(header);

		// greater than zero because if it is zero then there is no point of trimming
		if (headerStart > 0) {
			//trim off any leading characters
			header = header.substring(headerStart, header.length());
		}
		// This is used if there is garbage after the header on the same line
		if (header.startsWith(headerMarker) && !header.matches(headerMarker + "\\d.\\d")) {
			if (header.length() < headerMarker.length() + 3) {
				// No version number at all, set to 1.4 as default
				header = headerMarker + defaultVersion;
				LOG.warn("No version found, set to " + defaultVersion + " as default.");
			} else {
				String headerGarbage = header.substring(headerMarker.length() + 3, header.length()) + "\n";
				header = header.substring(0, headerMarker.length() + 3);
				source.rewind(headerGarbage.getBytes(ISO_8859_1).length);
			}
		}

		float headerVersion = 1.4f;
		try {
			String[] headerParts = header.split("-");
			if (headerParts.length == 2) {
				headerVersion = Float.parseFloat(headerParts[1]);
			}
		} catch (NumberFormatException exception) {
			LOG.warn("Can't parse the header version, default version is used.", exception);
		}
		document.setVersion(headerVersion);

		checkComment();

		// rewind
		source.seek(0);
		return true;
	}

	/**
	 * check second line of pdf header
	 */
	private void checkComment() throws IOException {
		String comment = readLine();
		boolean isValidComment = Boolean.TRUE;

		if (comment != null && !comment.isEmpty()) {
			if (comment.charAt(0) != '%') {
				isValidComment = Boolean.FALSE;
			}

			int pos = comment.indexOf('%') > -1 ? comment.indexOf('%') + 1 : 0;
			if (comment.substring(pos).length() < 4) {
				isValidComment = Boolean.FALSE;
			}
		} else {
			isValidComment = Boolean.FALSE;
		}
		if (isValidComment) {
			byte[] commentBytes = comment.getBytes();
			setBinaryHeaderBytes(commentBytes[1], commentBytes[2],
					commentBytes[3], commentBytes[4]);
		} else {
			setBinaryHeaderBytes(-1, -1, -1, -1);
		}
	}

	private void setBinaryHeaderBytes(int first, int second, int third, int fourth) {
		((ValidationCOSDocument) document).setHeaderCommentByte1(first);
		((ValidationCOSDocument) document).setHeaderCommentByte2(second);
		((ValidationCOSDocument) document).setHeaderCommentByte3(third);
		((ValidationCOSDocument) document).setHeaderCommentByte4(fourth);
	}

	private void checkStreamSpacings(ValidationCOSStream stream) throws IOException {
		int whiteSpace = source.read();
		if (whiteSpace == 13) {
			whiteSpace = source.read();
			if (whiteSpace != 10) {
				stream.setIsStreamSpacingsComplyPDFA(Boolean.FALSE);
				source.rewind(1);
			}
		} else if (whiteSpace != 10) {
			LOG.warn("Stream at " + source.getPosition() + " offset has no EOL marker.");
			stream.setIsStreamSpacingsComplyPDFA(Boolean.FALSE);
			source.rewind(1);
		}
	}

	private void checkEndStreamSpacings(ValidationCOSStream stream, long expectedLength) throws IOException {
		skipSpaces();

		byte eolCount = 0;
		long approximateLength = source.getPosition() - stream.getOriginLength();
		long diff = approximateLength - expectedLength;

		source.rewind(2);
		int firstSymbol = source.read();
		int secondSymbol = source.read();
		if (secondSymbol == 10) {
			if (firstSymbol == 13) {
				eolCount = (byte) (diff == 1 ? 1 : 2);
			} else {
				eolCount = 1;
			}
		} else if (secondSymbol == 13) {
			eolCount = 1;
		} else {
			LOG.warn("End of stream at " + source.getPosition() + " offset has no contain EOL marker.");
			stream.setIsEndStreamSpacingsComplyPDFA(Boolean.FALSE);
		}

		stream.setOriginLength(approximateLength - eolCount);
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

	public COSDictionary getFirstTrailer() {
		return xrefTrailerResolver.getFirstTrailer();
	}

	/**
	 * @return last trailer in current document
	 */
	public COSDictionary getLastTrailer() {
		return xrefTrailerResolver.getLastTrailer();
	}

	protected void isLinearized(Long fileLen) throws IOException {
		Map.Entry<COSObjectKey, Long> object = getFirstDictionary();

		final COSObject pdfObject = new COSObject(null);
		if (object != null) {
			parseFileObject(object.getValue(), object.getKey(), pdfObject);
		} else {
			LOG.warn("Linearization dictionary is missed in document");
			return ;
		}

		if (pdfObject.getObject() != null && pdfObject.getObject() instanceof COSDictionary) {
			final COSDictionary linearized = (COSDictionary) pdfObject.getObject();
			if (linearized.getItem(COSName.getPDFName("Linearized")) != null) {
				COSNumber length = (COSNumber) linearized.getItem(COSName.L);
				if (length != null) {
					boolean isLinearized = (length.longValue() == fileLen)
											&& source.getPosition() < LINEARIZATION_SIZE;
					((ValidationCOSDocument) document).setIsLinearized(isLinearized);
				}
			}
		}
	}

	private Map.Entry<COSObjectKey, Long> getFirstDictionary() throws IOException {
		source.seek(0L);
		skipSpaces();
		final int bound = Math.min(source.available(), LINEARIZATION_SIZE);

		for (long offset = source.getPosition(); offset < bound; offset++) {
			try {
				source.seek(offset);
				Long objNr = readObjectNumber();
				Integer genNr = readGenerationNumber();
				readExpectedString(OBJ_MARKER, Boolean.TRUE);
				return new AbstractMap.SimpleEntry<COSObjectKey, Long>(new COSObjectKey(objNr, genNr), offset);
			} catch (IOException ignore) {
				// if we`ve got trash instead of object or generation number, or 'obj' marker,
				// than we try to get it on next position
			}
		}
		return null;
	}

}
