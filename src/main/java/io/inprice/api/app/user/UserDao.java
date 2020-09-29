package io.inprice.api.app.user;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.UserMapper;
import io.inprice.common.models.User;

public interface UserDao {

  @SqlQuery("select * from user where id=:id")
  @UseRowMapper(UserMapper.class)
  User findById(@Bind("id") Long id);

  @SqlQuery("select * from user where email=:email")
  @UseRowMapper(UserMapper.class)
  User findByEmail(@Bind("email") String email);

  @SqlQuery("select name from user where email=:email")
  String findUserNameByEmail(@Bind("email") String email);

  @SqlUpdate("insert into user (email, name, timezone, password_salt, password_hash) values (:email, :name, :timezone, :salt, :hash)")
  @GetGeneratedKeys("id")
  long insert(@Bind("email") String email, @Bind("name") String name, @Bind("timezone") String timezone,
      @Bind("passwordSalt") String passwordSalt, @Bind("passwordHash") String passwordHash);

  @SqlUpdate("update user set password_salt=:passwordSalt, password_hash=:passwordHash where id=:id")
  boolean updatePassword(@Bind("id") Long id, @Bind("passwordSalt") String passwordSalt, @Bind("passwordHash") String passwordHash);

}
