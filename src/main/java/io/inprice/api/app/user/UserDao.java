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
  @SqlQuery("select id, email, full_name, timezone, privileged, banned, banned_at, ban_reason from user where email=:email")
  @UseRowMapper(UserMapper.class)
  User findByEmail(@Bind("email") String email);

  @SqlQuery("select * from user where email=:email")
  @UseRowMapper(UserMapper.class)
  User findByEmailWithPassword(@Bind("email") String email);

  @SqlQuery("select full_name from user where email=:email")
  String findFullNameByEmail(@Bind("email") String email);

  @SqlUpdate("insert into user (email, password, full_name, timezone) values (:email, :saltedHash, :fullName, :timezone)")
  @GetGeneratedKeys
  long insert(@Bind("fullName") String fullName, @Bind("email") String email, @Bind("saltedHash") String saltedHash, @Bind("timezone") String timezone);

  @SqlUpdate("update user set full_name=:fullName, timezone=:timezone where id=:id")
  boolean updateInfo(@Bind("id") Long id, @Bind("fullName") String fullName, @Bind("timezone") String timezone);

  @SqlUpdate("update user set password=:saltedHash where id=:id")
  boolean updatePassword(@Bind("id") Long id, @Bind("saltedHash") String saltedHash);

}
