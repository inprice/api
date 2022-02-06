package io.inprice.api.app.dashboard;

import java.util.List;
import java.util.Map;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import io.inprice.api.app.dashboard.mapper.MRU25Link;
import io.inprice.api.app.dashboard.mapper.MRU25LinkReducer;
import io.inprice.api.app.dashboard.mapper.PlatformSummary;
import io.inprice.api.app.dashboard.mapper.PlatformSummaryMapper;
import io.inprice.api.app.dashboard.mapper.ProductSummary;
import io.inprice.api.app.dashboard.mapper.ProductSummaryMapper;
import io.inprice.common.meta.Position;

interface DashboardDao {

  @SqlQuery("select grup, count(1) as counter from link where workspace_id=:workspaceId group by grup")
  @KeyColumn("grup")
  @ValueColumn("counter")
  Map<String, Integer> findGrupDists(@Bind("workspaceId") Long workspaceId);
  
  @SqlQuery("select position, count(1) as counter from product where workspace_id=:workspaceId group by position")
  @KeyColumn("position")
  @ValueColumn("counter")
  Map<String, Integer> findProductPositionDists(@Bind("workspaceId") Long workspaceId);

  @SqlQuery("select position, count(1) as counter from link where workspace_id=:workspaceId group by position")
  @KeyColumn("position")
  @ValueColumn("counter")
  Map<String, Integer> findLinkPositionDists(@Bind("workspaceId") Long workspaceId);

  @SqlQuery(
    "select l.id, l.product_id, p.name as product_name, pl.domain, l.seller, l.price, l.status, " +
    "l.parse_code, l.position, l.name, l.url, l.updated_at, l.created_at, l.url, l.alarm_id, " +
    "l.workspace_id, al.name as al_name, lp.new_price as lp_price from link as l " + 
		"inner join product as p on p.id = l.product_id " + 
    "left join platform as pl on pl.id = l.platform_id " + 
    "left join alarm as al on al.id = l.alarm_id " + 
    "left join link_price as lp on lp.link_id = l.id " + 
    "where l.workspace_id=:workspaceId and l.price > 0 " +
    "order by l.updated_at desc, l.status, lp.id " +
    "limit 25"
  )
  @UseRowReducer(MRU25LinkReducer.class)
  List<MRU25Link> findMR25Link(@Bind("workspaceId") Long workspaceId);

  @SqlQuery(
    "select * from product " +
    "where position=:position " +
    "  and workspace_id=:workspaceId " +
    "order by updated_at desc " +
    "limit <number>"
  )
  @UseRowMapper(ProductSummaryMapper.class)
  List<ProductSummary> findMostNProduct(@Define("number") int number, @Bind("position") Position position, @Bind("workspaceId") Long workspaceId);

  @SqlQuery(
    "select pl.domain, " + 
    "sum(if(grup = 'ACTIVE', 1, 0)) as actives, sum(if(grup = 'TRYING', 1, 0)) as tryings, " + 
    "sum(if(grup = 'WAITING', 1, 0)) as waitings, sum(if(grup = 'PROBLEM', 1, 0)) as problems from link as l " +
    "inner join platform as pl on pl.id = l.platform_id " + 
    "where workspace_id=:workspaceId " +
    "group by pl.domain "
  )
  @UseRowMapper(PlatformSummaryMapper.class)
  List<PlatformSummary> findPlatformStatusDist(@Bind("workspaceId") Long workspaceId);

}
