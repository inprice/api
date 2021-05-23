package io.inprice.api.app.superuser.dto;

public enum ALOrderDir {

	ASC(""),
	DESC(" desc");

  private String dir;
  
	private ALOrderDir(String dir) {
		this.dir =  dir;
	}
	
	public String getDir() {
		return dir;
	}
	
}
