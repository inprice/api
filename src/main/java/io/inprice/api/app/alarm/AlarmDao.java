package io.inprice.api.app.alarm;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.common.mappers.AlarmMapper;
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
	boolean removeAlarm(@Define("table") String table, @Bind("alarmId") Long alarmId, @Bind("workspaceId") Long workspaceId);

	@SqlUpdate(
		"update <table> " +
		"set alarmed_at=null, tobe_alarmed=false " +
		"where alarm_id=:alarmId " +
		"  and workspace_id=:workspaceId"
	)
	boolean resetAlarm(@Define("table") String table, @Bind("alarmId") Long alarmId, @Bind("workspaceId") Long workspaceId);

}
