package io.inprice.api.app.auth.dto;

import java.io.Serializable;
import java.util.Date;

import io.inprice.common.meta.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvitationSendDTO implements Serializable {

   private static final long serialVersionUID = 2545602928755294073L;

   private Long workspaceId;
   private String email;
   private UserRole role;
   private Date createdAt = new Date();

}
