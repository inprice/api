package io.inprice.api.app.company;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.CompanyMapper;
import io.inprice.common.mappers.UserMapper;
import io.inprice.common.models.Company;
import io.inprice.common.models.User;

interface CompanyDao {

  @SqlQuery("select * from company where id=:id")
  @UseRowMapper(CompanyMapper.class)
  Company findById(@Bind Long id);

  @SqlQuery("select * from company where admin_id=:adminId")
  @UseRowMapper(CompanyMapper.class)
  Company findByAdminId(@Bind Long adminId);

  @SqlQuery("select * from company where name=:name and admin_id=:adminId")
  @UseRowMapper(CompanyMapper.class)
  Company findByNameAndAdminId(@Bind String name, @Bind Long adminId);

  @SqlUpdate(
    "insert into company (admin_id, name, currency_code, currency_format) " + 
    "values (:adminId, :name, :currencyCode, :currencyFormat)"
  )
  @GetGeneratedKeys("id")
  long insertCompany(@Bind Long adminId, @Bind String name, @Bind String currencyCode, @Bind String currencyFormat);

  @SqlUpdate("update company set name=:name, currency_code=:currencyCode, currency_format=:currencyFormat where id=:id and admin_id=:adminId")
  boolean updateCompany(@Bind String name, @Bind String currencyCode, @Bind String currencyFormat, @Bind Long id, @Bind Long adminId);

  @SqlUpdate(
    "insert into user (email, name, timezone, password_salt, password_hash) " + 
    "values (:email, :name, :timezone, :salt, :hash)"
  )
  @GetGeneratedKeys("id")
  long insertUser(@Bind String email, @Bind String name, @Bind String timezone, @Bind String salt, @Bind String hash);

  @SqlUpdate(
    "insert into membership (user_id, email, company_id, role, status, updated_at) " + 
    "values (:userId, :email, :companyId, :role, :status, now())"
  )
  @GetGeneratedKeys("id")
  long insertMembership(@Bind Long userId, @Bind String email, @Bind Long companyId, @Bind String role, @Bind String status);

  @SqlQuery("select * from user where id=:id")
  @UseRowMapper(UserMapper.class)
  User findUserById(@Bind Long id);

  @SqlQuery("select * from user where email=:email")
  @UseRowMapper(UserMapper.class)
  User findUserByEmail(@Bind String email);

  @SqlQuery("select _hash from user_session where company_id=:companyId")
  List<String> getSessionHashesByCompanyId(@Bind Long companyId);



}
