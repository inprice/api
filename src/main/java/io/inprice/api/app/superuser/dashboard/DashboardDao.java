package io.inprice.api.app.superuser.dashboard;

import java.util.List;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.info.Pair;
import io.inprice.common.mappers.NameCountPairMapper;

public interface DashboardDao {

  @SqlQuery("select status as name, count(1) as _count from link group by status")
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findLinkStatusCounts();

  @SqlQuery("select grup as name, count(1) as _count from link group by grup")
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findLinkGrupCounts();

  @SqlQuery("select status as name, count(1) as _count from workspace group by status")
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findWorkspaceStatusCounts();

  @SqlQuery("select timezone as name, count(1) as _count from user group by timezone")
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findUserTimezoneCounts();

  @SqlQuery("select position as name, count(1) as _count from product group by position")
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findProductPositionCounts();

  @SqlQuery(
    "select pl.domain as name, count(1) as _count from link as l " +
    "inner join platform as pl on pl.id = l.platform_id " +
    "group by pl.domain " +
    "order by _count desc, name " +
    "limit 10"
  )
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findPlatformCounts();

  @SqlQuery("select status as name, count(1) as _count from ticket group by status")
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findTicketStatusCounts();

  @SqlQuery("select priority as name, count(1) as _count from ticket group by priority")
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findTicketPriorityCounts();

  @SqlQuery("select type as name, count(1) as _count from ticket group by type")
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findTicketTypeCounts();

  @SqlQuery("select subject as name, count(1) as _count from ticket group by subject")
  @UseRowMapper(NameCountPairMapper.class)
  List<Pair<String, Integer>> findTicketSubjectCounts();

}
