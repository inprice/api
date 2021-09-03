package io.inprice.api.app.superuser.platform;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.superuser.platform.dto.PlatformDTO;
import io.inprice.common.mappers.PlatformMapper;
import io.inprice.common.models.Link;

public interface PlatformDao {

  @SqlQuery("select * from platform where id=:id")
  @UseRowMapper(PlatformMapper.class)
  Link findById(@Bind("id") Long id);

  @SqlUpdate(
		"update platform " + 
		"set name=:dto.name, currency_code=:dto.currencyCode, currency_format=:dto.currencyFormat, queue=:dto.queue, profile=:dto.profile, parked=:dto.parked, blocked=:dto.blocked " +
		"where id=:dto.id"
	)
  boolean update(@BindBean("dto") PlatformDTO dto);

  @SqlUpdate("update platform set parked = not parked where id=:id")
  boolean toggleParked(@Bind("id") Long id);

  @SqlUpdate("update platform set blocked = not blocked where id=:id")
  boolean toggleBlocked(@Bind("id") Long id);

}
