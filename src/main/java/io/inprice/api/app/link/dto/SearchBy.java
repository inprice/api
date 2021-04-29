package io.inprice.api.app.link.dto;

public enum SearchBy {

	SKU("sku"),
  NAME("name"),
  BRAND("brand"),
  SELLER("seller"),
  PLATFORM("domain");

  private String fieldName;
  
	private SearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
