package io.inprice.api.meta;

public enum OrderDir {

	ASC(""),
	DESC(" desc");

  private String dir;
  
	private OrderDir(String dir) {
		this.dir =  dir;
	}
	
	public String getDir() {
		return dir;
	}
	
}
