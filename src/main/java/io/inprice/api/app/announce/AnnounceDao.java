package io.inprice.api.app.announce;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.AnnounceMapper;
import io.inprice.common.models.Announce;

public interface AnnounceDao {

  @SqlUpdate(
		"insert into announce_log (announce_id, user_id, workspace_id) " + 
		"select a.id, <userId>, <workspaceId> from announce a " +
		"where (a.type='SYSTEM' " +
      		"or (a.type='USER' and a.user_id=<userId>) " +
      		"or (a.type='WORKSPACE' and a.workspace_id=<workspaceId>)" +
      	") " +
		"  and (now() between a.starting_at and a.ending_at) " +
		"  and a.id not in (select announce_id from announce_log l where l.user_id=<userId>) "
	)
  boolean addLogsForWaitingAnnounces(@Define("userId") Long userId, @Define("workspaceId") Long workspaceId);

  @SqlUpdate(
		"insert into announce_log (announce_id, user_id, workspace_id) " + 
		"values (:announceId, :userId, :workspaceId) "
	)
  boolean markAsRead(@Bind("announceId") Long announceId, @Bind("userId") Long userId, @Bind("workspaceId") Long workspaceId);

  @SqlQuery(
		"select * from announce a " + 
		"where (a.type='SYSTEM' " +
  				"or (a.type='USER' and a.user_id=:userId) " +
  				"or (a.type='WORKSPACE' and a.workspace_id=:workspaceId)" +
				") " +
		"  and a.id not in (select announce_id from announce_log l where l.user_id=:userId) " +
		"  and (now() between a.starting_at and a.ending_at) " +
		"order by a.type, a.ending_at desc " +
		"limit 12"
	)
  @UseRowMapper(AnnounceMapper.class)
  List<Announce> fetchNotLoggedAnnounces(@Bind("userId") Long userId, @Bind("workspaceId") Long workspaceId);

}
