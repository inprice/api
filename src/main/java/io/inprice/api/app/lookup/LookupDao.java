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
  long insert(@Bind("companyId") Long companyId, @Bind("type") String type, @Bind("name") String name);

  @SqlQuery("select * from lookup where company_id=:companyId and type=:type and name=:name")
  @UseRowMapper(LookupMapper.class)
  Lookup findByTypeAndName(@Bind("type") String type, @Bind("name") String name, @Bind("companyId") Long companyId);

  @SqlQuery("select * from lookup where company_id=:companyId and type=:type order by name")
  @UseRowMapper(LookupMapper.class)
  List<Lookup> findListByCompanyIdAndType(@Bind("companyId") Long companyId, @Bind("type") String type);

  @SqlQuery(
    "select l.id, l.name, count(1) as counter from lookup as l " +
    "inner join product as p on p.<type>_id = l.id " +
    "where p.company_id=:companyId " +
    "group by l.id, l.name " +
    "order by l.name "
  )
  @UseRowMapper(LookupWithInfoMapper.class)
  List<LookupWithInfo> findLookupsWithInfo(@Define("type") String type, @Bind("companyId") Long companyId);

}
