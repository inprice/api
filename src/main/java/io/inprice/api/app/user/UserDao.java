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

  @SqlUpdate("insert into user (email, password, name, timezone) values (:email, :saltedHash, :name, :timezone)")
  @GetGeneratedKeys
  long insert(@Bind("email") String email, @Bind("saltedHash") String saltedHash, @Bind("name") String name, @Bind("timezone") String timezone);

  @SqlUpdate("update user set name=:name, timezone=:timezone where id=:id")
  boolean updateName(@Bind("id") Long id, @Bind("name") String name, @Bind("timezone") String timezone);

  @SqlUpdate("update user set password=:saltedHash where id=:id")
  boolean updatePassword(@Bind("id") Long id, @Bind("saltedHash") String saltedHash);

}
