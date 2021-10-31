package io.inprice.api.meta;

public enum ReportType {

	Product_Links("product", "links"),
	Product_Prices("product", "prices"),
	Product_Groups("product", "groups");

	private String group;
	private String fileName;

	private ReportType(String group, String fileName) {
		this.group = group;
		this.fileName = fileName;
	}

	public String getGroup() {
		return group;
	}

	public String getFileName() {
		return fileName;
	}
	
	public String getFilePath(ReportUnit unit) {
		if (unit.equals(ReportUnit.Pdf)) {
			return String.format(REPORT_FILE_PATH, this.group, this.fileName + ".jasper");
		} else {
			return String.format(REPORT_FILE_PATH, this.group, this.fileName + "_csv.jasper");
		}
	}

	private static final String REPORT_FILE_PATH = "reports/%s/%s";
	
}
