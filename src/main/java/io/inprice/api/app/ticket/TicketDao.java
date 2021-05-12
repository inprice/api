package io.inprice.api.app.ticket;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.TicketDTO;
import io.inprice.common.mappers.TicketMapper;
import io.inprice.common.meta.TicketCSatLevel;
import io.inprice.common.models.Ticket;

public interface TicketDao {

	@SqlUpdate(
		"insert into ticket (type, subject, query, link_id, group_id, user_id, account_id) " +
		"values (:dto.type, :dto.subject, :dto.query, :dto.linkId, :dto.groupId, :dto.userId, :dto.accountId)"
	)
	boolean insert(@BindBean("dto") TicketDTO dto);

	@SqlUpdate("update ticket set type=:dto.type, subject=:dto.subject, query=:dto.query where id=:dto.id")
	boolean update(@BindBean("dto") TicketDTO dto);

	@SqlQuery("select * from ticket where id=:id and account_id=:accountId")
	@UseRowMapper(TicketMapper.class)
	Ticket findById(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlUpdate("delete from ticket where id=:id and replied_at is null")
  boolean delete(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlQuery("select * from ticket where account_id=:accountId order by created_at desc")
  @UseRowMapper(TicketMapper.class)
  List<Ticket> getList(@Bind("accountId") Long accountId);

	@SqlUpdate("update ticket set csat_level=:level, csat_reason=:reason, csated_at=now() where id=:id")
	boolean setCSatLevel(@Bind("id") Long id, @Bind("level") TicketCSatLevel level, @Bind("reason") String reason);

}
