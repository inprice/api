package io.inprice.api.app.link.dto;

public enum SearchBy {

	Name("l.name"),
	Seller("seller"),
	Brand("brand"),
	SKU("sku"),
  Platform("domain");

  private String fieldName;
  
	private SearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}

}
