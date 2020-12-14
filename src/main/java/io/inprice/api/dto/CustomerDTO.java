package io.inprice.api.dto;

import java.io.Serializable;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stripe.model.InvoiceLineItem;

import io.inprice.common.meta.CompanyStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CustomerDTO implements Serializable {

  private static final long serialVersionUID = -8632826821980873263L;

  private String email;
  private String title;
  private String address1;
  private String address2;
  private String postcode;
  private String city;
  private String state;
  private String country;

  private String subsId;
  private String custId;
  private String planName;
  private CompanyStatus companyStatus;
  private Timestamp renewalDate;

  @JsonIgnore
  private transient InvoiceLineItem lineItem; //from stripe

}
