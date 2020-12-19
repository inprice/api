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

  @SqlQuery("select status, count(1) as counter from link where import_detail_id is null and account_id=:accountId group by status")
  @KeyColumn("status")
  @ValueColumn("counter")
  Map<String, Integer> findStatusDists(@Bind("accountId") Long accountId);

  @SqlQuery("select position, count(1) as counter from product where account_id=:accountId group by position")
  @KeyColumn("position")
  @ValueColumn("counter")
  Map<Integer, Integer> findPositionDists(@Bind("accountId") Long accountId);

  @SqlQuery(
    "select p.name as product_name, l.platform, l.seller, l.price, l.status, l.url, l.last_update, l.created_at, l.url from link as l " + 
    "inner join product as p on p.id = l.product_id " + 
    "where l.import_detail_id is null " +
    "  and l.account_id=:accountId " +
    "order by l.status, l.last_update desc " +
    "limit 25"
  )
  @UseRowMapper(MRU25LinkMapper.class)
  List<MRU25Link> findMR25Link(@Bind("accountId") Long accountId);

  @SqlQuery(
    "select id, name, price, updated_at, created_at, ranking, ranking_with from product " +
    "where position=:position " +
    "  and account_id=:accountId " +
    "order by updated_at desc " +
    "limit 10"
  )
  @UseRowMapper(Most10ProductMapper.class)
  List<Most10Product> findMost10Product(@Bind("position") Integer position, @Bind("accountId") Long accountId);

}
