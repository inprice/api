package io.inprice.api.app.alarm;

import java.util.List;
import java.util.Set;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.api.app.alarm.mapper.AlarmEntity;
import io.inprice.api.app.alarm.mapper.AlarmEntityMapper;
import io.inprice.common.mappers.AlarmMapper;
import io.inprice.common.mappers.IdNamePairMapper;
import io.inprice.common.meta.AlarmTopic;
import io.inprice.common.models.Alarm;

public interface AlarmDao {

	@SqlQuery("select * from alarm where id=:id and workspace_id=:workspaceId")
  @UseRowMapper(AlarmMapper.class)
	Alarm findById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select exists(" +
			"select 1 from alarm " +
			"where id != :id " +
			"  and name = :name " +
			"  and workspace_id = :workspaceId " +
		")")
	boolean doesExistByName(@Bind("name") String name, @Bind("id") Long id, @Bind("workspaceId") Long workspaceId); //id is for update

	@SqlUpdate(
		"insert into alarm (name, topic, subject, subject_when, certain_position, amount_lower_limit, amount_upper_limit, workspace_id) " +
		"values (:dto.name, :dto.topic, :dto.subject, :dto.subjectWhen, :dto.certainPosition, :dto.amountLowerLimit, :dto.amountUpperLimit, :dto.workspaceId)"
	)
	@GetGeneratedKeys
	long insert(@BindBean("dto") AlarmDTO dto);

	@SqlUpdate(
		"update alarm " +
		"set name=:dto.name, subject=:dto.subject, subject_when=:dto.subjectWhen, certain_position=:dto.certainPosition, " +
		"amount_lower_limit=:dto.amountLowerLimit, amount_upper_limit=:dto.amountUpperLimit, updated_at=now() " +
		"where id=:dto.id " +
		"  and workspace_id=:dto.workspaceId"
	)
	boolean update(@BindBean("dto") AlarmDTO dto);

  @SqlUpdate(
		"delete from alarm " +
		"where id=:id  " +
		"  and workspace_id=:workspaceId"
	)
  boolean delete(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

	@SqlUpdate(
		"update <table> " +
		"set alarm_id=null, tobe_alarmed=false " +
		"where alarm_id=:alarmId " +
		"  and workspace_id=:workspaceId"
	)
	int removeAlarmById(@Define("table") String table, @Bind("alarmId") Long alarmId, @Bind("workspaceId") Long workspaceId);

	@SqlUpdate(
		"update <table> " +
		"set alarm_id=null, tobe_alarmed=false " +
		"where id in (<idSet>) " +
		"  and alarm_id is not null " +
		"  and workspace_id=:workspaceId"
	)
	int removeAlarmsByEntityIds(@Define("table") String table, @BindList("idSet") Set<Long> idSet, @Bind("workspaceId") Long workspaceId);

	@SqlUpdate(
		"update <table> " +
		"set alarmed_at=null, tobe_alarmed=false " +
		"where alarm_id=:alarmId " +
		"  and workspace_id=:workspaceId"
	)
	int resetAlarmById(@Define("table") String table, @Bind("alarmId") Long alarmId, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select id, name from alarm where topic=:topic and workspace_id = :workspaceId order by name")
  @UseRowMapper(IdNamePairMapper.class)
  List<IdNamePairMapper> getIdNameList(@Bind("topic") AlarmTopic topic, @Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select id, sku, name, position, price, alarmed_at, workspace_id, false as is_a_link from product " +
		"where alarm_id=:alarmId " +
		"  and workspace_id=:workspaceId " +
		"order by name"
	)
  @UseRowMapper(AlarmEntityMapper.class)
	List<AlarmEntity> findProductEntities(@Bind("alarmId") Long alarmId, @Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select id, sku, IFNULL(name, url) as name, position, price, alarmed_at, workspace_id, true as is_a_link from link " +
		"where alarm_id=:alarmId " +
		"  and workspace_id=:workspaceId " +
		"order by name"
	)
  @UseRowMapper(AlarmEntityMapper.class)
	List<AlarmEntity> findLinkEntities(@Bind("alarmId") Long alarmId, @Bind("workspaceId") Long workspaceId);

}
