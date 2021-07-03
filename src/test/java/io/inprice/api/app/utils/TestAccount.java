package io.inprice.api.app.utils;

public enum TestAccount {

	X("Has two extra users without a plan"),
	Y("Has two extra users without a plan"),
	Z("Has two extra users with Basic plan (no extra users allowed)"),
	S("Has one extra user with Pro plan (3 extra users allowed)");
	
	private String description;
	
	TestAccount(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
