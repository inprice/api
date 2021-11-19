package io.inprice.api.app.alarm.dto;

public enum OrderBy {

	NAME("name"),
	TOPIC("topic"),
	SUBJECT("subject"),
	WHEN("subject_when");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
