package io.inprice.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StripeCustomerDTO {

  private String id;
  private String title;

  private String line1;
  private String line2;
  private String postcode;
  private String state;
  private String city;

  // Two-letter country code (<a href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2</a>).
  private String country;
  
}