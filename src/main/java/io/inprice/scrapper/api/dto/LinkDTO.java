package io.inprice.scrapper.api.dto;

import java.io.Serializable;

public class LinkDTO implements Serializable {

  private static final long serialVersionUID = 4899893105959011844L;

  private String url;
  private Long productId;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  @Override
  public String toString() {
    return "[productId=" + productId + ", url=" + url + "]";
  }

}
