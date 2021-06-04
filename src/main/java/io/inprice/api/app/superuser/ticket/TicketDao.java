package io.inprice.api.app.superuser.ticket;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.TicketCommentDTO;
import io.inprice.common.mappers.TicketCommentMapper;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.models.Ticket;
import io.inprice.common.models.TicketComment;

public interface TicketDao {

  @SqlQuery(
		"select *, u.name as username, a.name as account from ticket t " +
		"inner join user u on u.id= t.user_id " +
		"inner join account a on a.id= t.account_id " +
		"where t.id=:id "
	)
  @UseRowMapper(TicketMapper.class)
  Ticket findById(@Bind("id") Long id);

	@SqlUpdate("update ticket set seen_by_super=<seen> where id=:id ")
	boolean toggleSeenBySuper(@Bind("id") Long id, @Define("seen") boolean seen);
  
  @SqlUpdate(
		"update ticket " +
		"set comment_count=comment_count+1, seen_by_user=false, seen_by_super=true " +
		"where id=:id " +
		"  and status!='CLOSED'"
	)
  boolean increaseCommentCount(@Bind("id") Long ticketId);

  @SqlUpdate(
		"update ticket " +
		"set comment_count=comment_count-1, seen_by_super=true " +
		"where id=:id " +
		"  and status!='CLOSED'"
	)
	boolean decreaseCommentCount(@Bind("id") Long ticketId);

	@SqlUpdate(
		"insert into ticket_comment (ticket_id, content, added_by_user, user_id, account_id) " +
		"values (:dto.ticketId, :dto.content, false, :dto.userId, :dto.accountId)"
	)
	boolean insertComment(@BindBean("dto") TicketCommentDTO dto);

	@SqlUpdate(
		"update ticket_comment " +
		"set content=:dto.content " +
		"where id=:dto.id " +
		"  and editable=true " +
		"  and added_by_user=false " +
		"  and account_id=:dto.accountId"
	)
	boolean updateComment(@BindBean("dto") TicketCommentDTO dto);

  @SqlUpdate(
		"delete from ticket_comment " +
		"where id=:id " +
		"  and editable=true " + 
		"  and added_by_user=false"
	)
  boolean deleteCommentById(@Bind("id") Long id);

  @SqlQuery("select * from ticket_comment where id=:id")
  @UseRowMapper(TicketCommentMapper.class)
  TicketComment findCommentById(@Bind("id") Long id);

  @SqlQuery(
		"select *, u.name as username from ticket_comment c " +
		"inner join user u on u.id= c.user_id " +
		"where ticket_id=:ticketId"
	)
  @UseRowMapper(TicketCommentMapper.class)
  List<TicketComment> fetchCommentListByTicketId(@Bind("ticketId") Long ticketId);

  @SqlUpdate("delete from ticket_comment where ticket_id=:ticketId")
  boolean deleteComments(@Bind("ticketId") Long ticketId);

	@SqlUpdate(
		"update ticket_comment " +
		"set editable=false " +
		"where ticket_id=:ticketId "
	)
	boolean makeAllCommentsNotEditable(@Bind("ticketId") Long ticketId); //used for locking all previously added comments!

	@SqlUpdate(
		"update ticket_comment " +
		"set editable=true " +
		"where ticket_id=:ticketId " +
		"  and id in " +
		"(select * from (select id from ticket_comment where id!=:commentId and ticket_id=:ticketId order by id desc limit 1) as t)"
	)
	boolean makeOnePreviousCommentEditable(@Bind("ticketId") Long ticketId, @Bind("commentId") Long commentId);

}
