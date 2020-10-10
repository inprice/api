package io.inprice.api.app.member.mapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActiveMember {
  
  private Long id;
  private String name;
  private String role;
  private String status;
  private String date;

}
