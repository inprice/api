package io.inprice.api.app.dashboard;

import java.util.List;
import java.util.Map;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.dashboard.mapper.MRU25Link;
import io.inprice.api.app.dashboard.mapper.MRU25LinkMapper;
import io.inprice.api.app.dashboard.mapper.Most10Product;
import io.inprice.api.app.dashboard.mapper.Most10ProductMapper;

interface DashboardDao {

  @SqlQuery("select status, count(1) as counter from link where company_id=:companyId group by status")
  @KeyColumn("status")
  @ValueColumn("counter")
  Map<String, Integer> findStatusDists(@Bind("companyId") Long companyId);

  @SqlQuery("select position, count(1) as counter from product where company_id=:companyId group by position")
  @KeyColumn("position")
  @ValueColumn("counter")
  Map<Integer, Integer> findPositionDists(@Bind("companyId") Long companyId);

  @SqlQuery(
    "select p.name as product_name, s.name as platform, l.seller, l.price, l.status, l.last_update, l.created_at, l.url from link as l " + 
    "inner join product as p on p.id = l.product_id " + 
    "left join site as s on s.id = l.site_id " + 
    "where l.company_id=:companyId " +
    "order by l.last_update desc " +
    "limit 25"
  )
  @UseRowMapper(MRU25LinkMapper.class)
  List<MRU25Link> findMR25Link(@Bind("companyId") Long companyId);

  @SqlQuery(
    "select id, name, price, updated_at, created_at, ranking, ranking_with from product " +
    "where position=:position " +
    "  and company_id=:companyId " +
    "order by updated_at desc " +
    "limit 10"
  )
  @UseRowMapper(Most10ProductMapper.class)
  List<Most10Product> findMost10Product(@Bind("position") Integer position, @Bind("companyId") Long companyId);

}
