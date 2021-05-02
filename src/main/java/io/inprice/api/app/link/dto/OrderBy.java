package io.inprice.api.app.link.dto;

public enum OrderBy {

	Name("l.name"),
	Seller("seller"),
	Brand("brand"),
	SKU("sku"),
  Platform("domain"),
  Level("l.level"),
  Price("l.price"),
  Last_Checked("checked_at"),
  Last_Updated("updated_at");

  private String fieldName;
  
	private OrderBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
}
