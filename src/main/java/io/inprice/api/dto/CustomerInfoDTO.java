package io.inprice.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CustomerInfoDTO implements Serializable {

  private static final long serialVersionUID = -8632826821980873263L;

  private String customerId;
  private String title;
  private String address1;
  private String address2;
  private String postcode;
  private String city;
  private String state;
  private String country;

}
