package io.inprice.api.app.company;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CompanyMapper;
import io.inprice.common.models.Company;

public interface CompanyDao {

  @SqlQuery("select * from company where id=:id")
  @UseRowMapper(CompanyMapper.class)
  Company findById(@Bind("id") Long id);

  @SqlQuery("select * from company where admin_id=:adminId")
  @UseRowMapper(CompanyMapper.class)
  Company findByAdminId(@Bind("adminId") Long adminId);

  @SqlQuery("select * from company where name=:name and admin_id=:adminId")
  @UseRowMapper(CompanyMapper.class)
  Company findByNameAndAdminId(@Bind("name") String name, @Bind("adminId") Long adminId);

  @SqlUpdate(
    "insert into company (admin_id, name, currency_code, currency_format) " + 
    "values (:adminId, :name, :currencyCode, :currencyFormat)"
  )
  @GetGeneratedKeys("id")
  long insert(@Bind("adminId") Long adminId, @Bind("name") String name, 
    @Bind("currencyCode") String currencyCode, @Bind("currencyFormat") String currencyFormat);

  @SqlUpdate("update company set name=:name, currency_code=:currencyCode, currency_format=:currencyFormat where id=:id and admin_id=:adminId")
  boolean update(@Bind("name") String name, @Bind("currencyCode") String currencyCode, 
    @Bind("currencyFormat") String currencyFormat, @Bind("id") Long id, @Bind("adminId") Long adminId);

}
