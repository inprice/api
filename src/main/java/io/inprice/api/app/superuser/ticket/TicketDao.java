package io.inprice.api.app.superuser.ticket;

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
		"insert into ticket (reply, is_issue, solved, parent_id, account_id) " +
		"values (:dto.text, false, :dto.solved, :dto.parentId, :dto.accountId)"
	)
	boolean insert(@BindBean("dto") TicketDTO dto);

	@SqlUpdate("update ticket set reply=:dto.text, solved=:dto.solved where id=:dto.id and updatable=true")
	boolean update(@BindBean("dto") TicketDTO dto);

	@SqlUpdate("update ticket set updatable=false where (id=:parentId or parent_id=:parentId) and updatable=true")
	boolean makeAllPrecedingsNotUpdatable(@Bind("parentId") Long parentId);

  @SqlUpdate(
		"delete from ticket " +
		"where id=:id  " +
		"  and updatable=true "
	)
  boolean delete(@Bind("id") Long id);

  @SqlQuery("select * from ticket where id=:id")
  @UseRowMapper(TicketMapper.class)
  Ticket findById(@Bind("id") Long id);

	@SqlQuery(
		"select * from ticket " +
		"where id=:id or parent_id=:id " +
		"order by created_at"
	)
	@UseRowMapper(TicketMapper.class)
	List<Ticket> findWholeById(@Bind("id") Long id);

  @SqlQuery("select * from ticket where parent_id is null and account_id=:accountId order by created_at desc")
  @UseRowMapper(TicketMapper.class)
  List<Ticket> getParentsList(@Bind("accountId") Long accountId);

}
