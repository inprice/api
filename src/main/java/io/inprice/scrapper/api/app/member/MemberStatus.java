package io.inprice.scrapper.api.app.member;

/**
 * InvitationStatus: when a user (invitor) invites another user (invitee)
 * 
 * first status of an invitation is PENDING
 * 
 * if the invitee accepts the invitation than status becomes JOINED
 * 
 * the invitee may leave from the company then status becomes LEFT
 * 
 * the invitee may reject the invitation then status becomes REJECTED
 * 
 * the invitor want to cancel the invitation then status becomes CANCELLED
 * 
 * the invitor may also want to pause the invitation then status becomes PAUSED
 * 
 */
public enum MemberStatus {

   PENDING, JOINED, LEFT, REJECTED, CANCELLED, PAUSED;

}