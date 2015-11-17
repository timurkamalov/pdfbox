package org.apache.pdfbox.pdmodel.validation;

import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.ValidationParser;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Timur Kamalov
 */
public class ValidationPDDocument extends PDDocument {

	public static ValidationPDDocument load(InputStream fileStream) throws IOException {
		RandomAccessBufferedFileInputStream raFile = new RandomAccessBufferedFileInputStream(fileStream);
		ValidationParser parser = new ValidationParser(raFile);
		parser.parse();
		return parser.getPDDocument();
	}

}
