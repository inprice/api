package io.inprice.api.app.ticket.dto;

public enum OrderBy {

	STATUS("status"),
	PRIORITY("priority"),
	TYPE("type"),
	SUBJECT("subject"),
	CREATED_AT("t.created_at");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
