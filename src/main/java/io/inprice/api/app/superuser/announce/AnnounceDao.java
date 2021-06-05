package io.inprice.api.app.superuser.announce;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import io.inprice.api.app.superuser.announce.dto.AnnounceDTO;

public interface AnnounceDao {

  @SqlUpdate(
		"insert into announce (type, level, title, content, lasted_at) " + 
		"values (:dto.type, :dto.level, :dto.title, :dto.content, :dto.lastedAt) "
	)
  boolean insert(@BindBean("dto") AnnounceDTO dto);

  @SqlUpdate(
		"update announce " + 
		"set type=:dto.type, level=:dto.level, title=:dto.title, content=:dto.content, lasted_at=:dto.lastedAt " +
		"where id=:dto.id "
	)
  boolean update(@BindBean("dto") AnnounceDTO dto);

  @SqlUpdate("delete from announce where id=:id")
  boolean delete(@Bind("id") Long id);

}
