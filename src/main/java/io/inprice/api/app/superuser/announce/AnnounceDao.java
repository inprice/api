package io.inprice.api.app.superuser.announce;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import io.inprice.api.app.announce.dto.AnnounceDTO;

public interface AnnounceDao {

  @SqlUpdate(
		"insert into announce (type, level, title, body, link, starting_at, ending_at, user_id, account_id) " + 
		"values (:dto.type, :dto.level, :dto.title, :dto.body, :dto.link, :dto.startingAt, :dto.endingAt, :dto.userId, :dto.accountId) "
	)
  boolean insert(@BindBean("dto") AnnounceDTO dto);

  @SqlUpdate(
		"update announce " + 
		"set type=:dto.type, level=:dto.level, title=:dto.title, body=:dto.body, link=:dto.link, starting_at=:dto.startingAt, ending_at=:dto.endingAt " +
		"where id=:dto.id "
	)
  boolean update(@BindBean("dto") AnnounceDTO dto);
  
  @SqlUpdate("delete from announce where id=:id")
  boolean delete(@Bind("id") Long id);

}
