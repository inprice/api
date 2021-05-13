package io.inprice.api.app.ticket.dto;

public enum SearchBy {

	TYPE("type"),
	SUBJECT("subject"),
	QUERY("query"),
	REPLY("reply");

  private String fieldName;

	private SearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}

	public String getFieldName() {
		return fieldName;
	}

}
