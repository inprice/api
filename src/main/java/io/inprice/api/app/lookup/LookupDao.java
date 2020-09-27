package io.inprice.api.app.lookup;

import java.util.List;
import java.util.Map;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.lookup.mapper.LookupWithInfo;
import io.inprice.api.app.lookup.mapper.LookupWithInfoMapper;
import io.inprice.common.mappers.LookupMapper;
import io.inprice.common.models.Lookup;

interface LookupDao {

  @SqlUpdate("insert into lookup (company_id, type, name) values (:companyId, :type, :name)")
  @GetGeneratedKeys("id")
  long insertLookup(@Bind Long companyId, @Bind String type, @Bind String name);

  @SqlQuery("select * from lookup where company_id=:companyId and type=:type and name=:name")
  @UseRowMapper(LookupMapper.class)
  Lookup findByTypeAndName(@Bind String type, @Bind String name, @Bind Long companyId);

  @SqlQuery("select * from lookup where company_id=:companyId and type=:type order by name")
  @UseRowMapper(LookupMapper.class)
  List<Lookup> getList(@Bind Long companyId, @Bind String type);

  @SqlQuery(
    "select pp.position, count(1) as counter from product as p " +
    "left join product_price as pp on pp.id=p.last_price_id " +
    "where p.company_id=:companyId " +
    "group by pp.position " +
    "order by pp.position "
  )
  @KeyColumn("position")
  @ValueColumn("counter")
  Map<Integer, Integer> findPositionDists(@Bind Long companyId);

  @SqlQuery(
    "select l.id, l.name, count(1) as counter from lookup as l " +
    "inner join product as p on p.<type>_id = l.id " +
    "where p.company_id=:companyId " +
    "group by l.id, l.name " +
    "order by l.name "
  )
  @UseRowMapper(LookupWithInfoMapper.class)
  List<LookupWithInfo> findLookupsWithInfo(@Define String type, @Bind Long companyId);

}
