package io.inprice.api.app.workspace;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.workspace.mapper.WorkspaceInfo;
import io.inprice.api.app.workspace.mapper.WorkspaceInfoMapper;
import io.inprice.api.dto.CustomerDTO;
import io.inprice.common.mappers.UserMarksMapper;
import io.inprice.common.mappers.WorkspaceMapper;
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.models.UserMarks;
import io.inprice.common.models.Workspace;

public interface WorkspaceDao {

  @SqlQuery(
		"select w.*, p.name as plan_name, p.user_limit, p.link_limit, p.alarm_limit from workspace as w " +
		"left join plan as p on p.id = w.plan_id " +
		"where w.id=:id"
	)
  @UseRowMapper(WorkspaceMapper.class)
  Workspace findById(@Bind("id") Long id);

  @SqlQuery("select * from workspace where admin_id=:adminId")
  @UseRowMapper(WorkspaceMapper.class)
  Workspace findByAdminId(@Bind("adminId") Long adminId);

  @SqlQuery("select * from workspace where name=:name and admin_id=:adminId")
  @UseRowMapper(WorkspaceMapper.class)
  Workspace findByNameAndAdminId(@Bind("name") String name, @Bind("adminId") Long adminId);

  @SqlUpdate(
    "insert into workspace (admin_id, name, currency_code, currency_format) " + 
    "values (:adminId, :name, :currencyCode, :currencyFormat)"
  )
  @GetGeneratedKeys
  long insert(@Bind("adminId") Long adminId, @Bind("name") String name, 
    @Bind("currencyCode") String currencyCode, @Bind("currencyFormat") String currencyFormat);

  @SqlUpdate("update workspace set name=:name, currency_code=:currencyCode, currency_format=:currencyFormat where id=:id and admin_id=:adminId")
  boolean update(@Bind("name") String name, @Bind("currencyCode") String currencyCode, 
    @Bind("currencyFormat") String currencyFormat, @Bind("id") Long id, @Bind("adminId") Long adminId);

  @SqlUpdate(
    "update workspace set "+
    "title=:dto.title, contact_name=:dto.contactName, tax_id=:dto.taxId, tax_office=:dto.taxOffice, address_1=:dto.address1, " +
    "address_2=:dto.address2, postcode=:dto.postcode, city=:dto.city, state=:dto.state, country=:dto.country " +
    "where id=:id"
  )
  boolean update(@BindBean("dto") CustomerDTO dto, @Bind("id") Long id);

  @SqlUpdate("update workspace set user_count=user_count+1 where id=:id")
  boolean incUserCount(@Bind("id") Long id);
  
  @SqlUpdate("update workspace set link_count=link_count+<count> where id=:id")
  boolean incLinkCount(@Bind("id") Long id, @Define("count") Integer count);
  
  @SqlUpdate("update workspace set alarm_count=alarm_count+1 where id=:id")
  boolean incAlarmCount(@Bind("id") Long id);

  @SqlUpdate("update workspace set alarm_count=alarm_count-1 where id=:id")
  boolean decAlarmCount(@Bind("id") Long id);

  @SqlQuery(
    "select w.id, w.name, u.email from workspace as w " +
    "inner join user as u on u.id = w.admin_id " +
    "where w.status='SUBSCRIBED' "+
    "  and w.subs_renewal_at <= now() - interval 3 day"
  )
  @UseRowMapper(WorkspaceInfoMapper.class)
  List<WorkspaceInfo> findExpiredSubscriberWorkspaceList();

  @SqlUpdate("insert into workspace_history (workspace_id, status) values (:workspaceId, :status)")
  boolean insertStatusHistory(@Bind("workspaceId") Long workspaceId, @Bind("status") WorkspaceStatus status);

  @SqlUpdate(
    "insert into workspace_history (workspace_id, status, plan_id) " +
    "values (:workspaceId, :status, :planId)"
  )
  boolean insertStatusHistory(@Bind("workspaceId") Long workspaceId, @Bind("status") String status, @Bind("planId") Integer planId);

  @SqlQuery("select * from user_marks where email=:email and mark=:mark")
  @UseRowMapper(UserMarksMapper.class)
  UserMarks findUserMarkByEmail(@Bind("email") String email, @Bind("mark") String mark);

  @SqlUpdate("insert into user_marks (email, mark, boolean_val) values (:email, :mark, :boolVal)")
  void addUserMark(@Bind("email") String email, @Bind("mark") String mark, @Bind("boolVal") boolean boolVal);

  @SqlQuery("select count(1) from workspace where admin_id=:adminId")
  int findWorkspaceCount(@Bind("adminId") Long adminId);

}
