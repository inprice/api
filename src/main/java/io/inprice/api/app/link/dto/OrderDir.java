package io.inprice.api.app.link.dto;

public enum OrderDir {

	Ascending(""),
	Descending(" desc");

  private String dir;
  
	private OrderDir(String dir) {
		this.dir =  dir;
	}
	
	public String getDir() {
		return dir;
	}
	
}
