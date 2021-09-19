package io.inprice.api.app.superuser.dto;

public enum ALSearchBy {

	PATH("path"),
	STATUS("status"),
	REQ_BODY("req_body"),
	RES_BODY("res_body"),
	IP("ip"),

	//these two are for general searching and not used at workspace and user access log search ops
	EMAIL("user_email"),
	WORKSPACE_NAME("workspace_name");

  private String fieldName;
  
	private ALSearchBy(String fieldName) {
		this.fieldName =  fieldName;
	}
	
	public String getFieldName() {
		return fieldName;
	}

}
