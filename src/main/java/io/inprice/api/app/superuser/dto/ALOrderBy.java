package io.inprice.api.app.superuser.dto;

public enum ALOrderBy {

	DATE("created_at"),
	PATH("path"),
	STATUS("status"),
	IP("ip"),
	ELAPSED("elapsed"),
	METHOD("method"),
	SLOW("slow");

  private String fieldName;
  
	private ALOrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
