package io.inprice.scrapper.api.app.link;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Link implements Serializable {

  private static final long serialVersionUID = 2206190984817621818L;

  private Long id;
  private String url;
  private String urlHash;
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
  private Date createdAt;

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

}
