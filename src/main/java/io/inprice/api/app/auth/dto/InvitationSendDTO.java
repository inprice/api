package io.inprice.api.app.auth.dto;

import java.io.Serializable;
import java.util.Date;

import io.inprice.common.meta.UserRole;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InvitationSendDTO implements Serializable {

   private static final long serialVersionUID = 2545602928755294073L;

   private Long accountId;
   private String email;
   private UserRole role;
   private Date createdAt = new Date();

}
