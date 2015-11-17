package org.apache.pdfbox.cos.validation;

import org.apache.pdfbox.cos.COSString;

import java.io.IOException;

/**
 * @author Timur Kamalov
 */
public class ValidationCOSString extends COSString {

	private Boolean isHex = Boolean.FALSE;
	private boolean containsOnlyHex = true;
	private Long hexCount = 0L;

	public ValidationCOSString(byte[] bytes) {
		super(bytes);
	}

	public ValidationCOSString(String text) {
		super(text);
	}

	public static ValidationCOSString parseHex(String hex) throws IOException {
		ValidationCOSString result = (ValidationCOSString) COSString.parseHex(hex);
		result.setIsHex(Boolean.TRUE);
		return result;
	}

	public Boolean isHex() {
		return isHex;
	}

	public void setIsHex(Boolean isHex) {
		this.isHex = isHex;
	}

	public boolean isContainsOnlyHex() {
		return containsOnlyHex;
	}

	public void setContainsOnlyHex(boolean isHexSymbols) {
		this.containsOnlyHex = isHexSymbols;
	}

	public Long getHexCount() {
		return hexCount;
	}

	public void setHexCount(Long hexCount) {
		this.hexCount = hexCount;
	}

}
