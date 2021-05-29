package io.inprice.api.app.ticket;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.TicketDTO;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.mappers.TicketReplyMapper;
import io.inprice.common.meta.TicketStatus;
import io.inprice.common.models.Ticket;
import io.inprice.common.models.TicketReply;

public interface TicketDao {

  @SqlQuery("select * from ticket where id=:id")
  @UseRowMapper(TicketMapper.class)
  Ticket findById(@Bind("id") Long id);

	@SqlUpdate(
		"insert into ticket (priority, type, subject, issue, user_id, account_id) " +
		"values (:dto.priority, :dto.type, :dto.subject, :dto.text, :dto.userId, :dto.accountId)"
	)
	boolean insert(@BindBean("dto") TicketDTO dto);

	@SqlUpdate(
		"update ticket " +
		"set priority=:dto.priority, type=:dto.type, subject=:dto.subject, issue=:dto.text " +
		"where id=:dto.id " +
		"  and status='OPENED' " +
		"  and account_id=:dto.accountId"
	)
	boolean update(@BindBean("dto") TicketDTO dto);

  @SqlUpdate(
		"delete from ticket " +
		"where id=:id  " +
		"  and status='OPENED' " +
		"  and account_id=:accountId "
	)
  boolean delete(@Bind("id") Long id);

	@SqlUpdate("update ticket set status=:status where id=:id")
	boolean changeStatus(@Bind("id") Long id, @Bind("status") TicketStatus status);

	@SqlUpdate(
		"insert into ticket_reply (ticket_id, reply, user_id, account_id) " +
		"values (:dto.ticketId, :dto.text, :dto.userId, :dto.accountId)"
	)
	boolean insertReply(@BindBean("dto") TicketDTO dto);

	@SqlUpdate(
		"update ticket_reply " +
		"set reply=:dto.text " +
		"where id=:dto.id " +
		"  and editable=true " +
		"  and account_id=:dto.accountId"
	)
	boolean updateReply(@BindBean("dto") TicketDTO dto);

  @SqlUpdate(
		"delete from ticket_reply " +
		"where id=:id " +
		"  and editable=true "
	)
  boolean deleteReply(@Bind("id") Long id);

  @SqlQuery("select * from ticket_reply where id=:id")
  @UseRowMapper(TicketReplyMapper.class)
  TicketReply findReplyById(@Bind("id") Long id);

  @SqlQuery("select * from ticket_reply where ticket_id=:ticketId order by created_at")
  @UseRowMapper(TicketReplyMapper.class)
  List<TicketReply> fetchReplyListByTicketId(@Bind("ticketId") Long ticketId);

  @SqlUpdate(
		"delete from ticket_reply " +
		"where ticket_id=:ticketId "
	)
  boolean deleteReplies(@Bind("ticketId") Long ticketId);

	@SqlUpdate(
		"update ticket_reply " +
		"set editable=false " +
		"where ticket_id=:ticketId "
	)
	boolean makeAllPriorRepliesNotEditable(@Bind("ticketId") Long ticketId);

	@SqlUpdate(
		"update ticket_reply " +
		"set editable=true " +
		"where ticket_id=:ticketId " +
		"  and id in (select id from ticket_reply where id!=:replyId and ticket_id=:ticketId order by id desc limit 1)"
	)
	boolean makeOnePreviousReplyEditable(@Bind("ticketId") Long ticketId, @Bind("replyId") Long replyId);

	@SqlUpdate(
		"insert into ticket_history (ticket_id, status, priority, type, subject, user_id, account_id) " +
		"values (:dto.ticketId, :dto.status, :dto.priority, :dto.type, :dto.subject, :dto.userId, :dto.accountId)"
	)
	boolean insertHistory(@BindBean("dto") TicketDTO dto);

  @SqlUpdate(
		"delete from ticket_history " +
		"where ticket_id=:ticketId "
	)
  boolean deleteHistory(@Bind("ticketId") Long ticketId);

}
