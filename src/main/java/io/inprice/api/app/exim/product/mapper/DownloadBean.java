package io.inprice.api.app.exim.product.mapper;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadBean implements Serializable {
  
	private static final long serialVersionUID = -8516254527989303743L;

	private String sku;
  private String name;
  private double price;
  private String brandName;
  private String categoryName;

}
