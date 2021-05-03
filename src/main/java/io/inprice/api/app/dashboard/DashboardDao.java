package io.inprice.api.app.dashboard;

import java.util.List;
import java.util.Map;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.dashboard.mapper.MRU25Link;
import io.inprice.api.app.dashboard.mapper.MRU25LinkMapper;
import io.inprice.api.app.dashboard.mapper.GroupSummary;
import io.inprice.api.app.dashboard.mapper.GroupSummaryMapper;
import io.inprice.common.meta.Level;

interface DashboardDao {

  @SqlQuery("select status_group, count(1) as counter from link where account_id=:accountId group by status_group")
  @KeyColumn("status_group")
  @ValueColumn("counter")
  Map<String, Integer> findStatusGroupDists(@Bind("accountId") Long accountId);
  
  @SqlQuery("select level, count(1) as counter from link_group where account_id=:accountId group by level")
  @KeyColumn("level")
  @ValueColumn("counter")
  Map<String, Integer> findGroupLevelDists(@Bind("accountId") Long accountId);

  @SqlQuery("select level, count(1) as counter from link where account_id=:accountId group by level")
  @KeyColumn("level")
  @ValueColumn("counter")
  Map<String, Integer> findLinkLevelDists(@Bind("accountId") Long accountId);

  @SqlQuery(
    "select g.name as group_name, p.domain as platform, l.seller, l.price, l.status, l.level, l.url, l.updated_at, l.created_at, l.url from link as l " + 
		"inner join link_group as g on g.id = l.group_id " + 
    "left join platform as p on p.id = l.platform_id " + 
    "where l.account_id=:accountId " +
    "order by l.status, l.updated_at desc " +
    "limit 25"
  )
  @UseRowMapper(MRU25LinkMapper.class)
  List<MRU25Link> findMR25Link(@Bind("accountId") Long accountId);

  @SqlQuery(
    "select * from link_group " +
    "where level=:level " +
    "  and account_id=:accountId " +
    "order by updated_at desc " +
    "limit <number>"
  )
  @UseRowMapper(GroupSummaryMapper.class)
  List<GroupSummary> findMostNGroup(@Define("number") int number, @Bind("level") Level level, @Bind("accountId") Long accountId);

}
