package io.inprice.scrapper.api.app.user;

/**
 * InvitationStatus: when a user (invitor) invites another user (invitee)
 * first status of an invitation is PENDING
 * if the invitee accepts the invitation than status becomes JOINED
 * the invitee may leave from the company then status becomes LEFT
 * 
 */
public enum UserStatus {

   PENDING,
   JOINED,
   PAUSED,
   LEFT;

}