package io.inprice.api.app.superuser.platform.dto;

public enum OrderBy {

	NAME("name"),
	DOMAIN("domain"),
	COUNTRY("country"),
	CLASS("class_name");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
