package io.inprice.api.dto;

import java.io.Serializable;

/**
 * Used for handling String type coming from client side
 */
public class StringDTO implements Serializable {

  private static final long serialVersionUID = -6720720553359171076L;

  private String value;

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = value;
   }

}
