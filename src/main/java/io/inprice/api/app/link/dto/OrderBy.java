package io.inprice.api.app.link.dto;

public enum OrderBy {

	SKU("sku"),
  NAME("name"),
  BRAND("brand"),
  SELLER("seller"),
  PLATFORM("domain"),
  LAST_CHECKED("checked_at"),
  LAST_UPDATED("updated_at");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
