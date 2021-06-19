package io.inprice.api.app.alarm.dto;

public enum OrderBy {

	TOPIC("topic"),
	NAME("name"),
	WHEN("subject_when"),
	NOTIFIED_AT("notified_at");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
