package io.inprice.api.app.ticket.dto;

public enum OrderBy {

	TYPE("type"),
	SUBJECT("subject"),
	CREATED_AT("created_at"),
	REPLIED_AT("replied_at");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
