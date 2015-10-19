package org.apache.pdfbox.cos.validation;

import org.apache.pdfbox.cos.COSDocument;

/**
 * @author Timur Kamalov
 */
public class ValidationCOSDocument extends COSDocument {

	private int postEOFDataSize;

	public int getPostEOFDataSize() {
		return postEOFDataSize;
	}

	public void setPostEOFDataSize(int postEOFDataSize) {
		this.postEOFDataSize = postEOFDataSize;
	}
}
