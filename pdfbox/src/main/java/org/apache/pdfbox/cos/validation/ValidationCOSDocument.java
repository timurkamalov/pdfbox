package org.apache.pdfbox.cos.validation;

import org.apache.pdfbox.cos.COSDocument;

/**
 * @author Timur Kamalov
 */
public class ValidationCOSDocument extends COSDocument {

	private long postEOFDataSize;

	private long headerOffset;
	private String header;
	private int headerCommentByte1;
	private int headerCommentByte2;
	private int headerCommentByte3;
	private int headerCommentByte4;

	private boolean isXrefEOLMarkersComplyPDFA = true;
	private boolean isSubsectionHeaderSpaceSeparated = true;

	private boolean isLinearized;

	public long getPostEOFDataSize() {
		return postEOFDataSize;
	}

	public void setPostEOFDataSize(long postEOFDataSize) {
		this.postEOFDataSize = postEOFDataSize;
	}

	public long getHeaderOffset() {
		return headerOffset;
	}

	public void setHeaderOffset(long headerOffset) {
		this.headerOffset = headerOffset;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public int getHeaderCommentByte1() {
		return headerCommentByte1;
	}

	public void setHeaderCommentByte1(int headerCommentByte1) {
		this.headerCommentByte1 = headerCommentByte1;
	}

	public int getHeaderCommentByte2() {
		return headerCommentByte2;
	}

	public void setHeaderCommentByte2(int headerCommentByte2) {
		this.headerCommentByte2 = headerCommentByte2;
	}

	public int getHeaderCommentByte3() {
		return headerCommentByte3;
	}

	public void setHeaderCommentByte3(int headerCommentByte3) {
		this.headerCommentByte3 = headerCommentByte3;
	}

	public int getHeaderCommentByte4() {
		return headerCommentByte4;
	}

	public void setHeaderCommentByte4(int headerCommentByte4) {
		this.headerCommentByte4 = headerCommentByte4;
	}

	public boolean isXrefEOLMarkersComplyPDFA() {
		return isXrefEOLMarkersComplyPDFA;
	}

	public void setIsXrefEOLMarkersComplyPDFA(boolean isXrefEOLMarkersComplyPDFA) {
		this.isXrefEOLMarkersComplyPDFA = isXrefEOLMarkersComplyPDFA;
	}

	public boolean isSubsectionHeaderSpaceSeparated() {
		return isSubsectionHeaderSpaceSeparated;
	}

	public void setIsSubsectionHeaderSpaceSeparated(boolean isSubsectionHeaderSpaceSeparated) {
		this.isSubsectionHeaderSpaceSeparated = isSubsectionHeaderSpaceSeparated;
	}

	public boolean isLinearized() {
		return isLinearized;
	}

	public void setIsLinearized(boolean isLinearized) {
		this.isLinearized = isLinearized;
	}

}
