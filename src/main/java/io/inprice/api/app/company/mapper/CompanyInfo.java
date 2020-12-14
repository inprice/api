package io.inprice.api.app.company.mapper;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CompanyInfo {
  
  private Long id;
  private Long name;
  private String email;
  private String custId;

}
