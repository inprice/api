package io.inprice.scrapper.api.dto;

import java.io.Serializable;

public class LinkDTO implements Serializable {

  private static final long serialVersionUID = 4899893105959011844L;

  private Long id;
  private String url;
  private Long productId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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
    return "[id=" + id + ", productId=" + productId + ", url=" + url + "]";
  }

}
