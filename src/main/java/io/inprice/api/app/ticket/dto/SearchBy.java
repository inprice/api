package io.inprice.api.app.ticket.dto;

public enum SearchBy {

	BODY("body"),
	ACCOUNT("a.name");

  private String fieldName;
  
	private SearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}

}
