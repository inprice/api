package io.inprice.api.app.superuser.link.dto;

public enum OrderBy {

	NAME("l.name"),
	SELLER("seller"),
	BRAND("brand"),
	SKU("sku"),
  PLATFORM("domain"),
  POSITION("l.position"),
  PRICE("l.price"),
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
