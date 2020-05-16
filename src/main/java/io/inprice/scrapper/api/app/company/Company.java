package io.inprice.scrapper.api.app.company;

import java.io.Serializable;
import java.util.Date;

import io.inprice.scrapper.api.app.plan.PlanStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Company implements Serializable {

  private static final long serialVersionUID = 1818360516258349831L;

  private Long id;
  private String name;
  private String currencyCode;
  private String currencyFormat;
  private Long adminId;
  private String planName;
  private PlanStatus planStatus;
  private Date dueDate;
  private Integer retry = 0;
  private Date lastCollectingTime;
  private Boolean lastCollectingStatus = Boolean.FALSE;
  private Date createdAt = new Date();

}
