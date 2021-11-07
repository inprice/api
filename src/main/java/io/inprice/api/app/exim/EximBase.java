package io.inprice.api.app.exim;

import org.apache.commons.lang3.StringUtils;

public class EximBase {

	protected String normalizeValue(String value) {
		return normalizeValue(value, false);
	}
	
  protected String normalizeValue(String value, boolean isLastCol) {
  	StringBuilder sb = new StringBuilder();
  	if (StringUtils.isNotBlank(value)) {
  		String newVal = value.replaceAll("\\\\\"", "\"").replaceAll("\\\\\'", "'");
			if (newVal.indexOf(',') >= 0) sb.append('"');
			sb.append(newVal);
			if (newVal.indexOf(',') >= 0) sb.append('"');
  	} else {
  		sb.append("");
  	}

  	if (isLastCol) {
			sb.append('\n');
		} else {
			sb.append(',');
		}
  	return sb.toString(); 
  }

}
