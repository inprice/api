package io.inprice.api.app.superuser.link.dto;

public enum SearchBy {

	NAME("l.name"),
	SELLER("seller"),
	BRAND("brand"),
	SKU("sku"),
  PLATFORM("domain");

  private String fieldName;
  
	private SearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}

}
