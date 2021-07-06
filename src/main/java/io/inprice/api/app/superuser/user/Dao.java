package io.inprice.api.app.superuser.user;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.auth.mapper.DBSessionMapper;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.IdNamePairMapper;
import io.inprice.common.mappers.MembershipMapper;
import io.inprice.common.mappers.UserMapper;
import io.inprice.common.mappers.UserMarkMapper;
import io.inprice.common.meta.UserMarkType;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;
import io.inprice.common.models.UserMark;

public interface Dao {

  @SqlQuery(
		"select * from user " +
		"where email like :dto.term " +
		"  and privileged = false " +
		"order by email " +
		"limit :dto.rowCount, :dto.rowLimit "
	)
  @UseRowMapper(UserMapper.class)
	List<User> search(@BindBean("dto") BaseSearchDTO dto);
	
	@SqlQuery("select * from user where id=:id and privileged = false")
  @UseRowMapper(UserMapper.class)
	User findById(@Bind("id") Long id);

  @SqlQuery(
		"select s.*, a.name as account_name from user_session s " +
		"inner join account a on a.id = s.account_id " +
		"where s.user_id=:userId " +
		"order by created_at"
	)
  @UseRowMapper(DBSessionMapper.class)
  List<ForDatabase> fetchSessionListById(@Bind("userId") Long userId);

  @SqlQuery(
		"select m.*, a.name as account_name, a.status as account_status from membership m " +
		"inner join account a on a.id = m.account_id "+
		"where user_id=:userId " +
		"order by role, created_at"
	)
  @UseRowMapper(MembershipMapper.class)
  List<Membership> fetchMembershipListById(@Bind("userId") Long userId);

	@SqlQuery("select * from user_mark where email=:email order by created_at desc")
  @UseRowMapper(UserMarkMapper.class)
  List<UserMark> fetchUsedServiceListByEmail(@Bind("email") String email);

	@SqlQuery(
		"select id, name from account " +
		"where id in (select account_id from membership where user_id=:userId) " +
		"order by name"
	)
  @UseRowMapper(IdNamePairMapper.class)
  List<Pair<Long, String>> fetchAccountListByUserId(@Bind("userId") Long userId);

	@SqlUpdate("update user set banned=true, ban_reason=:reason, banned_at=now() where id=:id")
	boolean ban(@Bind("id") Long id, @Bind("reason") String reason);

	@SqlUpdate("update user set banned=false, ban_reason=null, banned_at=null where id=:id")
	boolean revokeBan(@Bind("id") Long id);

  @SqlUpdate("update account set pre_status=status, status='BANNED', last_status_update=now() where admin_id=:userId")
  int banAllBoundAccountsOfUser(@Bind("userId") Long userId);

  @SqlUpdate("update account set status=pre_status, pre_status='BANNED', last_status_update=now() where admin_id=:userId")
  int revokeBanAllBoundAccountsOfUser(@Bind("userId") Long userId);

	@SqlQuery("select * from user_mark where id=:id")
  @UseRowMapper(UserMarkMapper.class)
  UserMark findUsedServiceById(@Bind("id") Long id);
	
	@SqlUpdate("delete from user_mark where id=:id")
	boolean deleteUsedService(@Bind("id") Long id);

	@SqlUpdate("update user_mark set whitelisted = not whitelisted where id=:id")
	boolean toggleUnlimitedUsedService(@Bind("id") Long id);

	@SqlQuery(
		"select s.user_id from user_session s " +
		"inner join user u on u.id = s.user_id " +
		"where s._hash=:hash"
	)
  Long findUserEmailBySessionHash(@Bind("hash") String hash);

	@SqlUpdate("delete from user_session where _hash=:hash")
  boolean deleteSession(@Bind("hash") String hash);
	
	@SqlUpdate("delete user_mark where email=:email and type=:type)")
	void removeUserMark(@Bind("email") String email, @Bind("type") UserMarkType type);

	@SqlUpdate("insert into user_mark (email, type, description) values (:email, :type)")
  void addUserMark(@Bind("email") String email, @Bind("type") UserMarkType type, @Bind("description") String description);

}
