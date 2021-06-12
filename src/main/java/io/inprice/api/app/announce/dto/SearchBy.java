package io.inprice.api.app.announce.dto;

public enum SearchBy {

	TITLE("title"),
	BODY("body");

  private String fieldName;
  
	private SearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}

}
