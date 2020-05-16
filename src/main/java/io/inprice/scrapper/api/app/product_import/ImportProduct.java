package io.inprice.scrapper.api.app.product_import;

import java.io.Serializable;
import java.util.Date;

import io.inprice.scrapper.api.app.link.LinkStatus;
import io.inprice.scrapper.api.dto.ProductDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ImportProduct implements Serializable {

  private static final long serialVersionUID = 1099808351397302194L;

  private Long id;
  private ImportType importType;
  private String data;
  private LinkStatus status = LinkStatus.NEW;
  private Date lastCheck;
  private Date lastUpdate;
  private Integer retry;
  private Integer httpStatus;
  private String description;
  private Long companyId;
  private Date createdAt;

  private ProductDTO productDTO;

}
