package io.inprice.scrapper.api.info;

public class Tokens {

	private String access;
	private String refresh;
	
	public Tokens(String access, String refresh) {
		this.access = access;
		this.refresh = refresh;
	}

	public String getAccess() {
		return access;
	}
	
	public void setAccess(String access) {
		this.access = access;
	}
	
	public String getRefresh() {
		return refresh;
	}
	
	public void setRefresh(String refresh) {
		this.refresh = refresh;
	}
	
}
