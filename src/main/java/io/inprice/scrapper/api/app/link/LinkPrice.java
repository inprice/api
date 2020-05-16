package io.inprice.scrapper.api.app.link;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LinkPrice implements Serializable {

   private static final long serialVersionUID = 6818942944451174569L;

   private Long id;
   private Long linkId;
   private BigDecimal price;
   private Long productId;
   private Long companyId;
   private Date createdAt;

   public LinkPrice(Long linkId, BigDecimal price) {
      this.linkId = linkId;
      this.price = price;
   }

}
