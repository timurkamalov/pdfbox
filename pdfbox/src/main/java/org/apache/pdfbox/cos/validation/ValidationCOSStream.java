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
	private Boolean isStreamSpacingsComplyPDFA = true;
	/** true if spacings around of 'endstream' keyword comply PDF/A standard
	 */
	private Boolean isEndStreamSpacingsComplyPDFA = true;

	public Long getOriginLength() {
		return originLength;
	}

	public void setOriginLength(Long originLength) {
		this.originLength = originLength;
	}

	public Boolean getIsStreamSpacingsComplyPDFA() {
		return isStreamSpacingsComplyPDFA;
	}

	public void setIsStreamSpacingsComplyPDFA(Boolean isStreamSpacingsComplyPDFA) {
		this.isStreamSpacingsComplyPDFA = isStreamSpacingsComplyPDFA;
	}

	public Boolean getIsEndStreamSpacingsComplyPDFA() {
		return isEndStreamSpacingsComplyPDFA;
	}

	public void setIsEndStreamSpacingsComplyPDFA(Boolean isEndStreamSpacingsComplyPDFA) {
		this.isEndStreamSpacingsComplyPDFA = isEndStreamSpacingsComplyPDFA;
	}

}
