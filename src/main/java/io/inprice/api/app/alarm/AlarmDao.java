package io.inprice.api.app.alarm;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import io.inprice.api.app.alarm.dto.AlarmDTO;

public interface AlarmDao {

	@SqlUpdate(
		"insert into alarm (group_id, link_id, subject, subject_when, certain_status, price_lower_limit, price_upper_limit, account_id) " +
		"values (:dto.groupId, :dto.linkId, :dto.subject, :dto.when, :dto.certainStatus, :dto.priceLowerLimit, :dto.priceUpperLimit, :dto.accountId)"
	)
	@GetGeneratedKeys
	long insert(@BindBean("dto") AlarmDTO dto);

	@SqlUpdate(
		"update alarm " +
		"set subject=:dto.subject, subject_when=:dto.when, certain_status=:dto.certainStatus, price_lower_limit=:dto.priceLowerLimit, price_upper_limit=:dto.priceUpperLimit " +
		"where id=:dto.id " +
		"  and account_id=:dto.accountId"
	)
	boolean update(@BindBean("dto") AlarmDTO dto);
	
  @SqlUpdate(
		"delete from alarm " +
		"where id=:id  " +
		"  and account_id=:accountId"
	)
  boolean delete(@Bind("id") Long id, @Bind("accountId") Long accountId);

}
