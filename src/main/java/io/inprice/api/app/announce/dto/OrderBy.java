package io.inprice.api.app.announce.dto;

public enum OrderBy {

	TITLE("title"),
	TYPE("type"),
	LEVEL("level"),
	STARTING_AT("starting_at"),
	ENDING_AT("ending_at"),
	CREATED_AT("created_at");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
