package io.inprice.api.app.alarm.dto;

public enum OrderBy {

	SUBJECT("subject"),
	WHEN("subject_when"),
	UPDATED_AT("a.updated_at"),
	TRIGGERED_AT("triggered_at");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
