package io.inprice.scrapper.api.app.link;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LinkHistory implements Serializable {

   private static final long serialVersionUID = 885057701505049080L;

   private Long id;
   private Long linkId;
   private LinkStatus status = LinkStatus.NEW;
   private Integer httpStatus;
   private Long productId;
   private Long companyId;
   private Date createdAt;

   public LinkHistory(LinkStatus status) {
      this.status = status;
   }

   public LinkHistory(Long linkId, LinkStatus status) {
      this.linkId = linkId;
      this.status = status;
   }

}
