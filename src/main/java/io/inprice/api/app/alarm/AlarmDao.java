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

	@SqlQuery("select * from alarm where id=:id and account_id=:accountId")
  @UseRowMapper(AlarmMapper.class)
	Alarm findById(@Bind("id") Long id, @Bind("accountId") Long accountId);

	@SqlQuery("select exists(select 1 from alarm where <topic>_id=:topicId and account_id=:accountId)")
	boolean doesExistByTopicId(@Define("topic") String topic, @Bind("topicId") Long topicId, @Bind("accountId") Long accountId);

	@SqlUpdate(
		"insert into alarm (topic, product_id, link_id, subject, subject_when, certain_status, amount_lower_limit, amount_upper_limit, last_status, last_amount, account_id) " +
		"values (:dto.topic, :dto.productId, :dto.linkId, :dto.subject, :dto.subjectWhen, :dto.certainStatus, :dto.amountLowerLimit, :dto.amountUpperLimit, :pair.left, :pair.right, :dto.accountId)"
	)
	@GetGeneratedKeys
	long insert(@BindBean("dto") AlarmDTO dto, @BindBean("pair") Pair<String, BigDecimal> pair);

	@SqlUpdate(
		"update alarm " +
		"set subject=:dto.subject, subject_when=:dto.subjectWhen, certain_status=:dto.certainStatus, amount_lower_limit=:dto.amountLowerLimit, " +
		"    amount_upper_limit=:dto.amountUpperLimit, last_status=:pair.left, last_amount=:pair.right, updated_at=now() " +
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
		"update product " +
		"set alarm_id=:alarmId " +
		"where id=:productId " +
		"  and account_id=:accountId"
	)
	boolean setAlarmForProduct(@Bind("productId") Long productId, @Bind("alarmId") Long alarmId, @Bind("accountId") Long accountId);

	@SqlUpdate(
		"update link " +
				"set alarm_id=:alarmId " +
		"where id=:linkId " +
		"  and account_id=:accountId"
	)
	boolean setAlarmForLink(@Bind("linkId") Long productId, @Bind("alarmId") Long alarmId, @Bind("accountId") Long accountId);

	@SqlUpdate(
		"update product " +
		"set alarm_id=null " +
		"where id=:productId " +
		"  and account_id=:accountId"
	)
	boolean removeAlarmFromProduct(@Bind("productId") Long productId, @Bind("accountId") Long accountId);

	@SqlUpdate(
		"update link " +
		"set alarm_id=null " +
		"where id=:linkId " +
		"  and account_id=:accountId"
	)
	boolean removeAlarmFromLink(@Bind("linkId") Long linkId, @Bind("accountId") Long accountId);

}
