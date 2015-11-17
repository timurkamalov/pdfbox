package org.apache.pdfbox.pdmodel.validation;

import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.ValidationParser;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

/**
 * @author Timur Kamalov
 */
public class ValidationPDDocument extends PDDocument {

	public static ValidationPDDocument load(File file) throws IOException {
		RandomAccessBufferedFileInputStream raFile = new RandomAccessBufferedFileInputStream(file);
		ValidationParser parser = new ValidationParser(raFile);
		parser.parse();
		return parser.getPDDocument();
	}

}
