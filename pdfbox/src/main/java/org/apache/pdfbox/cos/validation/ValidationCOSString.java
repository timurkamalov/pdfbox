package org.apache.pdfbox.cos.validation;

import org.apache.pdfbox.cos.COSString;

import java.io.IOException;

/**
 * @author Timur Kamalov
 */
public class ValidationCOSString extends COSString {

	private boolean containsOnlyHex = true;
	private Long hexCount = 0L;

	public ValidationCOSString(byte[] bytes) {
		super(bytes);
	}

	public static ValidationCOSString parseHex(String hex) throws IOException {
		COSString string = COSString.parseHex(hex);
		ValidationCOSString result = fromCOSString(string);
		return result;
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

	public static ValidationCOSString fromCOSString(COSString string) {
		ValidationCOSString result = new ValidationCOSString(string.getBytes());
		result.setIsHex(string.isHex());
		return result;
	}

}
