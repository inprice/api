package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EmailDTO implements Serializable {

   private static final long serialVersionUID = 5884990803032543745L;

   private String email;
   private Long companyId;

}
