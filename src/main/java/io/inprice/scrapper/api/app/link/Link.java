package io.inprice.scrapper.api.app.link;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class Link implements Serializable {

  private static final long serialVersionUID = 2206190984817621818L;

  private Long id;
  private String url;
  private String sku;
  private String name;
  private String brand;
  private String seller;
  private String shipment;
  private BigDecimal price = BigDecimal.ZERO;
  private Date lastUpdate;
  private Date lastCheck;
  private LinkStatus status = LinkStatus.NEW;
  private LinkStatus preStatus = LinkStatus.NEW;
  private Integer retry;
  private Integer httpStatus;
  private String websiteClassName;
  private Long productId;
  private Long siteId;
  private Long companyId;

  // these fields below are related to product importing!
  private Long importId;
  private Long importRowId;

  /**
   * The three list fields below never be saved into database.
   */
  private String platform;
  private List<LinkPrice> priceList;
  private List<LinkSpec> specList;
  private List<LinkHistory> historyList;

  /**
   * The field below never be saved into database.
   */
  private BigDecimal productPrice = BigDecimal.ZERO;

  public Link() {
  }

  public Link(String url) {
    this.url = url;
  }

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

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getSeller() {
    return seller;
  }

  public void setSeller(String seller) {
    this.seller = seller;
  }

  public String getShipment() {
    return shipment;
  }

  public void setShipment(String shipment) {
    this.shipment = shipment;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public Date getLastCheck() {
    return lastCheck;
  }

  public void setLastCheck(Date lastCheck) {
    this.lastCheck = lastCheck;
  }

  public LinkStatus getStatus() {
    return status;
  }

  public void setStatus(LinkStatus status) {
    this.preStatus = this.status;
    this.status = status;
  }

  public LinkStatus getPreStatus() {
    return preStatus;
  }

  public void setPreStatus(LinkStatus preStatus) {
    this.preStatus = preStatus;
  }

  public Integer getRetry() {
    return retry;
  }

  public void setRetry(Integer retry) {
    this.retry = retry;
  }

  public Integer getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(Integer httpStatus) {
    this.httpStatus = httpStatus;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public Long getSiteId() {
    return siteId;
  }

  public void setSiteId(Long siteId) {
    this.siteId = siteId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public String getWebsiteClassName() {
    return websiteClassName;
  }

  public void setWebsiteClassName(String websiteClassName) {
    this.websiteClassName = websiteClassName;
  }

  public List<LinkPrice> getPriceList() {
    return priceList;
  }

  public void setPriceList(List<LinkPrice> priceList) {
    this.priceList = priceList;
  }

  public List<LinkSpec> getSpecList() {
    return specList;
  }

  public void setSpecList(List<LinkSpec> specList) {
    this.specList = specList;
  }

  public List<LinkHistory> getHistoryList() {
    return historyList;
  }

  public void setHistoryList(List<LinkHistory> historyList) {
    this.historyList = historyList;
  }

  public BigDecimal getProductPrice() {
    return productPrice;
  }

  public void setProductPrice(BigDecimal productPrice) {
    this.productPrice = productPrice;
  }

  public Long getImportId() {
    return importId;
  }

  public void setImportId(Long importId) {
    this.importId = importId;
  }

  public Long getImportRowId() {
    return importRowId;
  }

  public void setImportRowId(Long importRowId) {
    this.importRowId = importRowId;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

}
