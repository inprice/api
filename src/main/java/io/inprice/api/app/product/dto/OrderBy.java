package io.inprice.api.app.product.dto;

public enum OrderBy {

	NAME("p.name"),
	SKU("sku"),
	CATEGORY("cat.name"),
	BRAND("brn.name");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
