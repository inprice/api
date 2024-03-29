package io.inprice.api.dto;

import java.io.Serializable;
import java.sql.Timestamp;

import io.inprice.common.meta.WorkspaceStatus;
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
  private String contactName;
  private String taxId;
  private String taxOffice;

  private String address1;
  private String address2;
  private String postcode;
  private String city;
  private String state;
  private String country;

  private Integer planId;
  private WorkspaceStatus workspaceStatus;
  private Timestamp subsRenewalDate;

}
