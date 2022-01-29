package io.inprice.api.app.superuser.workspace;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.system.PlanDao;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.WorkspaceHistoryMapper;
import io.inprice.common.mappers.WorkspaceMapper;
import io.inprice.common.mappers.WorkspaceTransMapper;
import io.inprice.common.mappers.IdNamePairMapper;
import io.inprice.common.mappers.MembershipMapper;
import io.inprice.common.models.Workspace;
import io.inprice.common.models.WorkspaceHistory;
import io.inprice.common.models.WorkspaceTrans;
import io.inprice.common.models.Membership;

public interface Dao {

  @SqlQuery(
		"select w.id, w.name, u.email, currency_code, country from workspace as w " +
		"inner join user as u on u.id = w.admin_id " +
		"where w.name like :dto.term " +
		"order by w.name " +
		"limit :dto.rowCount, :dto.rowLimit "
	)
  @UseRowMapper(WorkspaceMapper.class)
	List<Workspace> search(@BindBean("dto") BaseSearchDTO dto);
	
	@SqlQuery(
		"select w.*, u.email"+ PlanDao.FIELDS +" from workspace as w "+
		"inner join user as u on u.id = w.admin_id " +
		"left join plan as p on p.id = w.plan_id " +
		"where w.id=:id"
	)
  @UseRowMapper(WorkspaceMapper.class)
	Workspace findById(@Bind("id") Long id);

  @SqlQuery(
		"select m.*, w.name as workspace_name, w.status as workspace_status from membership m " +
		"inner join workspace w on w.id = m.workspace_id "+
		"where workspace_id=:workspace_id " +
		"order by role, created_at"
	)
  @UseRowMapper(MembershipMapper.class)
  List<Membership> fetchMemberList(@Bind("workspace_id") Long workspaceId);

  @SqlQuery(
		"select *, p.name as plan_name from workspace_history h "+
		"left join plan as p on p.id = h.plan_id " +
		"where h.workspace_id=:workspaceId "+
		"order by h.created_at desc"
	)
  @UseRowMapper(WorkspaceHistoryMapper.class)
  List<WorkspaceHistory> fetchHistory(@Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from workspace_trans where workspace_id=:workspaceId order by created_at desc")
  @UseRowMapper(WorkspaceTransMapper.class)
  List<WorkspaceTrans> fetchTransactionList(@Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select id, full_name as name from user " +
		"where id in (select user_id from membership where workspace_id=:workspaceId) " +
		"order by full_name"
	)
  @UseRowMapper(IdNamePairMapper.class)
  List<Pair<Long, String>> fetchUserListByWorkspaceId(@Bind("workspaceId") Long workspaceId);

  @SqlQuery("select id, name from workspace where name like :term order by name limit 15")
  @UseRowMapper(IdNamePairMapper.class)
  List<IdNamePairMapper> searchIdNameListByName(@Bind("term") String term);

  @SqlUpdate(
    "insert into voucher (code, plan_id, days, description, issued_id) " +
    "values (:code, :planId, :days, :description, :issuedId)"
  )
  boolean createVoucher(@Bind("code") String code, @Bind("planId") Integer planId,
    @Bind("days") Long days, @Bind("description") String description, @Bind("issuedId") Long issuedId);

}
