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
import io.inprice.api.app.dashboard.mapper.ProductSummary;
import io.inprice.api.app.dashboard.mapper.ProductSummaryMapper;
import io.inprice.common.meta.Level;

interface DashboardDao {

  @SqlQuery("select grup, count(1) as counter from link where workspace_id=:workspaceId group by grup")
  @KeyColumn("grup")
  @ValueColumn("counter")
  Map<String, Integer> findGrupDists(@Bind("workspaceId") Long workspaceId);
  
  @SqlQuery("select level, count(1) as counter from product where workspace_id=:workspaceId group by level")
  @KeyColumn("level")
  @ValueColumn("counter")
  Map<String, Integer> findProductLevelDists(@Bind("workspaceId") Long workspaceId);

  @SqlQuery("select level, count(1) as counter from link where workspace_id=:workspaceId group by level")
  @KeyColumn("level")
  @ValueColumn("counter")
  Map<String, Integer> findLinkLevelDists(@Bind("workspaceId") Long workspaceId);

  @SqlQuery(
    "select l.id, g.name as product_name, p.domain as platform, l.seller, l.price, l.status, l.parse_code, l.level, l.name, l.url, l.updated_at, l.created_at, l.url from link as l " + 
		"inner join product as g on g.id = l.product_id " + 
    "left join platform as p on p.id = l.platform_id " + 
    "where l.workspace_id=:workspaceId " +
    "order by l.updated_at, l.status " +
    "limit 10"
  )
  @UseRowMapper(MRU25LinkMapper.class)
  List<MRU25Link> findMR25Link(@Bind("workspaceId") Long workspaceId);

  @SqlQuery(
    "select * from product " +
    "where level=:level " +
    "  and workspace_id=:workspaceId " +
    "order by updated_at desc " +
    "limit <number>"
  )
  @UseRowMapper(ProductSummaryMapper.class)
  List<ProductSummary> findMostNProduct(@Define("number") int number, @Bind("level") Level level, @Bind("workspaceId") Long workspaceId);

}
