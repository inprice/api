package io.inprice.api.app.company;

import java.sql.Timestamp;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.CustomerDTO;
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

  @SqlQuery("select * from company where subs_customer_id=:subsCustomerId")
  @UseRowMapper(CompanyMapper.class)
  Company findBySubsCustomerId(@Bind("subsCustomerId") String subsCustomerId);

  @SqlUpdate(
    "insert into company (admin_id, name, currency_code, currency_format) " + 
    "values (:adminId, :name, :currencyCode, :currencyFormat)"
  )
  @GetGeneratedKeys
  long insert(@Bind("adminId") Long adminId, @Bind("name") String name, 
    @Bind("currencyCode") String currencyCode, @Bind("currencyFormat") String currencyFormat);

  @SqlUpdate("update company set name=:name, currency_code=:currencyCode, currency_format=:currencyFormat where id=:id and admin_id=:adminId")
  boolean update(@Bind("name") String name, @Bind("currencyCode") String currencyCode, 
    @Bind("currencyFormat") String currencyFormat, @Bind("id") Long id, @Bind("adminId") Long adminId);

  @SqlUpdate(
    "update company set "+
    "title=:dto.title, address_1=:dto.address1, address_2=:dto.address2, postcode=:dto.postcode, city=:dto.city, state=:dto.state, country=:dto.country " +
    "where id=:id"
  )
  boolean update(@BindBean("dto") CustomerDTO dto, @Bind("id") Long id);

  @SqlUpdate(
    "update company " +
    "set title=:dto.title, address_1=:dto.address1, address_2=:dto.address2, postcode=:dto.postcode, city=:dto.city, state=:dto.state, country=:dto.country, " +
    "plan_name=:dto.planName, subs_id=:dto.subsId, subs_customer_id=:dto.subsCustomerId, subs_renewal_at=:dto.subsRenewalAt, subs_status=:subsStatus " +
    "where id=:id"
  )
  boolean update(@BindBean("dto") CustomerDTO dto, @Bind("subsStatus") String subsStatus, @Bind("id") Long id);
  
  @SqlUpdate("update company set product_count=product_count+1 where id=:id and product_count<product_limit")
  boolean increaseProductCountById(@Bind("id") Long id);

  @SqlUpdate("update company set subs_status=:subsStatus, subs_renewal_at=:subsRenewalAt where id=:id")
  boolean updateSubscription(@Bind("subsStatus") String subsStatus, @Bind("subsRenewalAt") Timestamp subsRenewalAt, @Bind("id") Long id);

  @SqlUpdate(
    "update company " + 
    "set free_usage=true, plan_name=:planName, subs_status=:subsStatus, subs_renewal_at=DATE_ADD(now(), interval <interval> day), product_limit=:productLimit " +
    "where id=:companyId"
  )
  boolean updateSubscription(@Bind("companyId") Long companyId, @Bind("subsStatus") String subsStatus, 
    @Bind("planName") String planName, @Bind("productLimit") Integer productLimit, @Define("interval") Integer interval);

}
