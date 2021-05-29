package io.inprice.api.app.ticket.dto;

public enum SearchBy {

	ISSUE("issue"),
	REPLY("reply");

  private String fieldName;

	private SearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}

	public String getFieldName() {
		return fieldName;
	}

}
