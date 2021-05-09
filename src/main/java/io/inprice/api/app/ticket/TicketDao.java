package io.inprice.api.app.ticket;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.TicketDTO;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.models.Ticket;

public interface TicketDao {
	
	@SqlUpdate(
		"insert into ticket (type, subject, query, link_id, group_id, user_id, account_id) " +
		"values (:dto.type, :dto.subject, :dto.query, :dto.linkId, :dto.groupId, :dto.userId, :dto.accountId)"
	)
	boolean insert(@BindBean("dto") TicketDTO dto);
	
	@SqlQuery("select * from ticket where id=:id and account_id=:accountId")
	@UseRowMapper(TicketMapper.class)
	Ticket findById(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlUpdate("delete from ticket where id=:id and replied_at is null")
  boolean delete(@Bind("id") Long id, @Bind("accountId") Long accountId);
  
  @SqlUpdate("update ticket set is_read=!is_read where id=:id and user_id=:userId")
  boolean markAsRead(@Bind("id") Long id, @Bind("userId") Long userId);

  @SqlUpdate("update ticket set is_read=true where is_read=false and user_id=:userId")
  boolean markAllAsRead(@Bind("userId") Long userId);

  @SqlQuery("select * from ticket where (query like :term or reply like :term) and account_id = :accountId order by created_at desc")
  @UseRowMapper(TicketMapper.class)
  List<Ticket> search(@Bind("term") String term, @Bind("accountId") Long accountId);

  @SqlQuery("select * from ticket where account_id=:accountId order by created_at desc")
  @UseRowMapper(TicketMapper.class)
  List<Ticket> getList(@Bind("accountId") Long accountId);

  @SqlQuery("select * from ticket where is_read=false and account_id=:accountId order by created_at desc")
  @UseRowMapper(TicketMapper.class)
  List<Ticket> findUnreadList(@Bind("accountId") Long accountId);

}
