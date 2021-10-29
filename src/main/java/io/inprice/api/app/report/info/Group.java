package io.inprice.api.app.report.info;

public enum Group {
	
	POSITION("p.position", "Position"),
	BRAND("brn.name", "Brand"),
	CATEGORY("cat.name", "Category");

	private String field;
	private String label;

	private Group(String field, String label) {
		this.field = field;
		this.label = label;
	}

	public String getField() {
		return field;
	}

	public String getLabel() {
		return label;
	}
}
