package io.inprice.api.app.alarm.dto;

public enum SearchBy {

	NAME("name");

  private String fieldName;
  
	private SearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}

}
