package io.inprice.api.app.ticket;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.TicketDTO;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.mappers.TicketCommentMapper;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.models.Ticket;
import io.inprice.common.models.TicketComment;

public interface TicketDao {

  @SqlQuery(
		"select *, email from ticket t " +
		"inner join user u on u.id= t.user_id " +
		"where t.id=:id"
	)
  @UseRowMapper(TicketMapper.class)
  Ticket findById(@Bind("id") Long id);

	@SqlUpdate(
		"insert into ticket (priority, type, subject, issue, user_id, account_id) " +
		"values (:dto.priority, :dto.type, :dto.subject, :dto.issue, :dto.userId, :dto.accountId)"
	)
	@GetGeneratedKeys
	long insert(@BindBean("dto") TicketDTO dto);

	@SqlUpdate(
		"update ticket " +
		"set priority=:dto.priority, type=:dto.type, subject=:dto.subject, issue=:dto.issue " +
		"where id=:dto.id " +
		"  and status='OPENED' " +
		"  and account_id=:dto.accountId"
	)
	boolean update(@BindBean("dto") TicketDTO dto);

  @SqlUpdate(
		"delete from ticket " +
		"where id=:id  " +
		"  and status='OPENED' "
	)
  boolean delete(@Bind("id") Long id);

	@SqlUpdate("update ticket set status=:status where id=:id")
	boolean changeStatus(@Bind("id") Long id, @Bind("status") TicketStatus status);

	@SqlUpdate(
		"insert into ticket_comment (ticket_id, content, user_id, account_id) " +
		"values (:dto.ticketId, :dto.issue, :dto.userId, :dto.accountId)"
	)
	boolean insertComment(@BindBean("dto") TicketDTO dto);

	@SqlUpdate(
		"update ticket_comment " +
		"set content=:dto.issue " +
		"where id=:dto.id " +
		"  and editable=true " +
		"  and account_id=:dto.accountId"
	)
	boolean updateComment(@BindBean("dto") TicketDTO dto);

  @SqlUpdate("delete from ticket_comment where id=:id  and editable=true")
  boolean deleteCommentById(@Bind("id") Long id);

  @SqlUpdate("delete from ticket_comment where ticket_id=:ticketId")
  boolean deleteCommentByTicketId(@Bind("ticketId") Long ticketId);

  @SqlQuery("select * from ticket_comment where id=:id")
  @UseRowMapper(TicketCommentMapper.class)
  TicketComment findCommentById(@Bind("id") Long id);

  @SqlQuery(
		"select *, email from ticket_comment c " +
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
	boolean makeAllCommentsNotEditable(@Bind("ticketId") Long ticketId);

	@SqlUpdate(
		"update ticket_comment " +
		"set editable=true " +
		"where ticket_id=:ticketId " +
		"  and id in (select id from ticket_comment where id!=:commentId and ticket_id=:ticketId order by id desc limit 1)"
	)
	boolean makeOnePreviousCommentEditable(@Bind("ticketId") Long ticketId, @Bind("commentId") Long commentId);

	@SqlUpdate(
		"insert into ticket_history (ticket_id, status, priority, type, subject, user_id, account_id) " +
		"values (:dto.id, :dto.status, :dto.priority, :dto.type, :dto.subject, :dto.userId, :dto.accountId)"
	)
	boolean insertHistory(@BindBean("dto") TicketDTO dto);

  @SqlUpdate("delete from ticket_history where ticket_id=:ticketId")
  boolean deleteHistoryByTicketId(@Bind("ticketId") Long ticketId);

}
