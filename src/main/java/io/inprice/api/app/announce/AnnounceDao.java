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
		"insert into announce_log (announce_id, user_id, account_id) " + 
		"select a.id, <userId>, <accountId> from announce a " +
		"where (now() between a.starting_at and a.ending_at) " +
		"  and not exists (select l.id from announce_log l where l.announce_id=a.id) "
	)
  boolean addLogsForWaitingAnnounces(@Define("userId") Long userId, @Define("accountId") Long accountId);

  @SqlQuery(
		"select * from announce a " + 
		"where (a.type='SYSTEM " +
  				"or (a.type='USER' and a.user_id=:userId) " +
  				"or (a.type='ACCOUNT' and a.account_id=:accountId)" +
				") " +
		"  and (now() between a.starting_at and a.ending_at) " +
		"  and not exists (select l.id from announce_log l where l.announce_id=a.id) " +
		"order by a.type, a.ending_at desc " +
		"limit 12"
	)
  @UseRowMapper(AnnounceMapper.class)
  List<Announce> fetchNotLoggedAnnounces(@Bind("userId") Long userId, @Bind("accountId") Long accountId);

}
