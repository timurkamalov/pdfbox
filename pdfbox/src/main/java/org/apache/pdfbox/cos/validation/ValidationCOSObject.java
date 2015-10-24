package org.apache.pdfbox.cos.validation;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSObject;

import java.io.IOException;

/**
 * @author Timur Kamalov
 */
public class ValidationCOSObject extends COSObject {

	private Boolean headerOfObjectComplyPDFA = true;
	private Boolean endOfObjectComplyPDFA = true;
	private Boolean headerFormatComplyPDFA = true;

	public ValidationCOSObject(COSBase object) throws IOException {
		super(object);
	}

	public Boolean getHeaderOfObjectComplyPDFA() {
		return headerOfObjectComplyPDFA;
	}

	public void setHeaderOfObjectComplyPDFA(Boolean headerOfObjectComplyPDFA) {
		this.headerOfObjectComplyPDFA = headerOfObjectComplyPDFA;
	}

	public Boolean getEndOfObjectComplyPDFA() {
		return endOfObjectComplyPDFA;
	}

	public void setEndOfObjectComplyPDFA(Boolean endOfObjectComplyPDFA) {
		this.endOfObjectComplyPDFA = endOfObjectComplyPDFA;
	}

	public Boolean getHeaderFormatComplyPDFA() {
		return headerFormatComplyPDFA;
	}

	public void setHeaderFormatComplyPDFA(Boolean headerFormatComplyPDFA) {
		this.headerFormatComplyPDFA = headerFormatComplyPDFA;
	}

}
