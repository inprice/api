package io.inprice.api.app.utils;

public enum TestAccount {

	X("Has two extra users without a plan"),
	Y("Has two extra users without a plan"),
	Z("Has two extra users with Basic plan"),
	S("Has one extra user with Standard plan");
	
	private String description;
	
	TestAccount(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
