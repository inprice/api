package io.inprice.api.app.alarm;

import java.math.BigDecimal;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.alarm.dto.AlarmDTO;
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.AlarmMapper;
import io.inprice.common.models.Alarm;

public interface AlarmDao {

	@SqlQuery("select * from alarm where id=:id and account_id=:accountId")
  @UseRowMapper(AlarmMapper.class)
	Alarm findById(@Bind("id") Long id, @Bind("accountId") Long accountId);

	@SqlUpdate(
		"insert into alarm (topic, group_id, link_id, subject, subject_when, certain_status, price_lower_limit, price_upper_limit, last_status, last_price, account_id) " +
		"values (:dto.topic, :dto.groupId, :dto.linkId, :dto.subject, :dto.subjectWhen, :dto.certainStatus, :dto.priceLowerLimit, :dto.priceUpperLimit, :pair.left, :pair.right, :dto.accountId)"
	)
	@GetGeneratedKeys
	long insert(@BindBean("dto") AlarmDTO dto, @BindBean("pair") Pair<String, BigDecimal> pair);

	@SqlUpdate(
		"update alarm " +
		"set subject=:dto.subject, subject_when=:dto.subjectWhen, certain_status=:dto.certainStatus, price_lower_limit=:dto.priceLowerLimit, price_upper_limit=:dto.priceUpperLimit, " +
		"    last_status=:pair.left, last_price=:pair.right, tobe_notified=false " +
		"where id=:dto.id " +
		"  and account_id=:dto.accountId"
	)
	boolean update(@BindBean("dto") AlarmDTO dto, @BindBean("pair") Pair<String, BigDecimal> pair);
	
  @SqlUpdate(
		"delete from alarm " +
		"where id=:id  " +
		"  and account_id=:accountId"
	)
  boolean delete(@Bind("id") Long id, @Bind("accountId") Long accountId);

	@SqlUpdate(
		"update link_group " +
		"set alarm_id=:alarmId " +
		"where id=:groupId " +
		"  and account_id=:accountId"
	)
	boolean setAlarmForGroup(@Bind("groupId") Long groupId, @Bind("alarmId") Long alarmId, @Bind("accountId") Long accountId);

	@SqlUpdate(
		"update link " +
				"set alarm_id=:alarmId " +
		"where id=:linkId " +
		"  and account_id=:accountId"
	)
	boolean setAlarmForLink(@Bind("linkId") Long groupId, @Bind("alarmId") Long alarmId, @Bind("accountId") Long accountId);

	@SqlUpdate(
		"update link_group " +
		"set alarm_id=null " +
		"where id=:groupId " +
		"  and account_id=:accountId"
	)
	boolean removeAlarmFromGroup(@Bind("groupId") Long groupId, @Bind("accountId") Long accountId);

	@SqlUpdate(
		"update link " +
		"set alarm_id=null " +
		"where id=:linkId " +
		"  and account_id=:accountId"
	)
	boolean removeAlarmFromLink(@Bind("linkId") Long linkId, @Bind("accountId") Long accountId);

}
