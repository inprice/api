package io.inprice.api.app.superuser.dto;

public enum ALOrderBy {

	DATE("created_at"),
	IP("ip"),
	PATH("path"),
	STATUS("status"),
	ELAPSED("elapsed"),
	METHOD("method"),
	SLOW("is_slow");

  private String fieldName;
  
	private ALOrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
