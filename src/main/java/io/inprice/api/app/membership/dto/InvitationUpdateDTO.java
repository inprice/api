package io.inprice.api.app.membership.dto;

import java.io.Serializable;

import io.inprice.common.meta.UserRole;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InvitationUpdateDTO implements Serializable {

   private static final long serialVersionUID = -7922528699777216078L;

   private Long id;
   private UserRole role;

}