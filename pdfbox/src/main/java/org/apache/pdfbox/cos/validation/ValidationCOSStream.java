package org.apache.pdfbox.cos.validation;

import org.apache.pdfbox.cos.COSStream;

/**
 * @author Timur Kamalov
 */
public class ValidationCOSStream extends COSStream {

	/** calculated length of stream. correct only if spacings comply PDF/A standard
	 */
	private Long originLength = 0L;
	/** true if spacings around of 'stream' keyword comply PDF/A standard
	 */
	private Boolean streamSpacingsComplyPDFA = true;
	/** true if spacings around of 'endstream' keyword comply PDF/A standard
	 */
	private Boolean endStreamSpacingsComplyPDFA = true;

	public Long getOriginLength() {
		return originLength;
	}

	public void setOriginLength(Long originLength) {
		this.originLength = originLength;
	}

	public Boolean getStreamSpacingsComplyPDFA() {
		return streamSpacingsComplyPDFA;
	}

	public void setStreamSpacingsComplyPDFA(Boolean streamSpacingsComplyPDFA) {
		this.streamSpacingsComplyPDFA = streamSpacingsComplyPDFA;
	}

	public Boolean getEndStreamSpacingsComplyPDFA() {
		return endStreamSpacingsComplyPDFA;
	}

	public void setEndStreamSpacingsComplyPDFA(Boolean endStreamSpacingsComplyPDFA) {
		this.endStreamSpacingsComplyPDFA = endStreamSpacingsComplyPDFA;
	}

}
