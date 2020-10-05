package io.inprice.api.app.user;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.UserMapper;
import io.inprice.common.models.User;

/**
 * Be careful what you return from select queries since passwordSalt and passwordHash are dangerous fields!
 * 
 */
public interface UserDao {

  @SqlQuery("select * from user where id=:id")
  @UseRowMapper(UserMapper.class)
  User findById(@Bind("id") Long id);

  //password related columns are excluded!
  @SqlQuery("select id, email, name, timezone from user where email=:email")
  @UseRowMapper(UserMapper.class)
  User findByEmail(@Bind("email") String email);

  @SqlQuery("select * from user where email=:email")
  @UseRowMapper(UserMapper.class)
  User findByEmailWithPassword(@Bind("email") String email);

  @SqlQuery("select name from user where email=:email")
  String findUserNameByEmail(@Bind("email") String email);

  @SqlUpdate("insert into user (email, name, timezone, password_salt, password_hash) values (:email, :name, :timezone, :salt, :hash)")
  @GetGeneratedKeys
  long insert(@Bind("email") String email, @Bind("name") String name, @Bind("timezone") String timezone,
      @Bind("salt") String passwordSalt, @Bind("hash") String passwordHash);

  @SqlUpdate("update user set name=:name, timezone=:timezone where id=:id")
  boolean updateName(@Bind("id") Long id, @Bind("name") String name, @Bind("timezone") String timezone);

  @SqlUpdate("update user set password_salt=:salt, password_hash=:hash where id=:id")
  boolean updatePassword(@Bind("id") Long id, @Bind("salt") String salt, @Bind("hash") String hash);

}
