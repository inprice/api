package io.inprice.api.meta;

public enum ReportUnit {

	Pdf(".pdf", "application/pdf", 961),
	Excel(".xls", "application/vnd.ms-excel", 4230),
	Csv(".csv", "text/csv", 0);

	private String fileExtention;
	private String contentType;
	private int emptyLength;

	private ReportUnit(String fileExtention, String contentType, int emptyLength) {
		this.fileExtention = fileExtention;
		this.contentType = contentType;
		this.emptyLength = emptyLength;
	}

	public String getFileExtention() {
		return fileExtention;
	}

	public String getContentType() {
		return contentType;
	}

	public int getEmptyLength() {
		return emptyLength;
	}

}
