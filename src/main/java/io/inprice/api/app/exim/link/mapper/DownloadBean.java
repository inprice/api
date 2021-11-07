package io.inprice.api.app.exim.link.mapper;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadBean implements Serializable {
  
	private static final long serialVersionUID = -7672734341532864209L;

	private String sku;
  private String url;

}
