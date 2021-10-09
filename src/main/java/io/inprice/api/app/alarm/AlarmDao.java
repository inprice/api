package io.inprice.api.app.alarm;

import java.math.BigDecimal;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.AlarmMapper;
import io.inprice.common.models.Alarm;

public interface AlarmDao {

	@SqlQuery("select * from alarm where id=:id and workspace_id=:workspaceId")
  @UseRowMapper(AlarmMapper.class)
	Alarm findById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

	@SqlQuery("select exists(select 1 from alarm where <topic>_id=:topicId and workspace_id=:workspaceId)")
	boolean doesExistByTopicId(@Define("topic") String topic, @Bind("topicId") Long topicId, @Bind("workspaceId") Long workspaceId);

	@SqlUpdate(
		"insert into alarm (topic, product_id, link_id, subject, subject_when, certain_position, amount_lower_limit, amount_upper_limit, last_position, last_amount, workspace_id) " +
		"values (:dto.topic, :dto.productId, :dto.linkId, :dto.subject, :dto.subjectWhen, :dto.certainPosition, :dto.amountLowerLimit, :dto.amountUpperLimit, :pair.left, :pair.right, :dto.workspaceId)"
	)
	@GetGeneratedKeys
	long insert(@BindBean("dto") AlarmDTO dto, @BindBean("pair") Pair<String, BigDecimal> pair);

	@SqlUpdate(
		"update alarm " +
		"set subject=:dto.subject, subject_when=:dto.subjectWhen, certain_position=:dto.certainPosition, amount_lower_limit=:dto.amountLowerLimit, " +
		"    amount_upper_limit=:dto.amountUpperLimit, last_position=:pair.left, last_amount=:pair.right, updated_at=now() " +
		"where id=:dto.id " +
		"  and workspace_id=:dto.workspaceId"
	)
	boolean update(@BindBean("dto") AlarmDTO dto, @BindBean("pair") Pair<String, BigDecimal> pair);
	
  @SqlUpdate(
		"delete from alarm " +
		"where id=:id  " +
		"  and workspace_id=:workspaceId"
	)
  boolean delete(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

	@SqlUpdate(
		"update product " +
		"set alarm_id=:alarmId " +
		"where id=:productId " +
		"  and workspace_id=:workspaceId"
	)
	boolean setAlarmForProduct(@Bind("productId") Long productId, @Bind("alarmId") Long alarmId, @Bind("workspaceId") Long workspaceId);

	@SqlUpdate(
		"update link " +
				"set alarm_id=:alarmId " +
		"where id=:linkId " +
		"  and workspace_id=:workspaceId"
	)
	boolean setAlarmForLink(@Bind("linkId") Long productId, @Bind("alarmId") Long alarmId, @Bind("workspaceId") Long workspaceId);

	@SqlUpdate(
		"update product " +
		"set alarm_id=null " +
		"where id=:productId " +
		"  and workspace_id=:workspaceId"
	)
	boolean removeAlarmFromProduct(@Bind("productId") Long productId, @Bind("workspaceId") Long workspaceId);

	@SqlUpdate(
		"update link " +
		"set alarm_id=null " +
		"where id=:linkId " +
		"  and workspace_id=:workspaceId"
	)
	boolean removeAlarmFromLink(@Bind("linkId") Long linkId, @Bind("workspaceId") Long workspaceId);

}
