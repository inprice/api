package io.inprice.api.app.ticket.dto;

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
