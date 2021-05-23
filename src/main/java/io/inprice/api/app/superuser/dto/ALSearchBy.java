package io.inprice.api.app.superuser.dto;

public enum ALSearchBy {

	PATH("path"),
	STATUS("status"),
	REQ_BODY("req_body"),
	RES_BODY("res_body"),
	IP("ip"),

	//these two are for general searching and not used at account and user access log search ops
	EMAIL("user_email"),
	ACCOUNT_NAME("account_name");

  private String fieldName;
  
	private ALSearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}

}
