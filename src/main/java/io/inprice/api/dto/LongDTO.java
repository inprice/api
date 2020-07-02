package io.inprice.api.dto;

import java.io.Serializable;

/**
 * Used for handling Long type coming from client side
 */
public class LongDTO implements Serializable {

   private static final long serialVersionUID = -7409381497867977979L;

   private Long value;

   public Long getValue() {
      return value;
   }

   public void setValue(Long value) {
      this.value = value;
   }

}
