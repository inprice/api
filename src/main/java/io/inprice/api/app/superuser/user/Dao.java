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
import io.inprice.common.mappers.MemberMapper;
import io.inprice.common.mappers.UserMapper;
import io.inprice.common.mappers.UserUsedMapper;
import io.inprice.common.models.Member;
import io.inprice.common.models.User;
import io.inprice.common.models.UserUsed;

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
	
	@SqlQuery(
		"select * from user "+
		"  and privileged = false " +
		"where id=:id"
	)
  @UseRowMapper(UserMapper.class)
	User findById(@Bind("id") Long id);

	@SqlQuery("select * from user_used where email=:email order by created_at desc")
  @UseRowMapper(UserUsedMapper.class)
  List<UserUsed> fetchUsedListByEmail(@Bind("email") String email);

  @SqlQuery(
		"select s.*, a.name as account_name from user_session s " +
		"inner join account a on a.id = s.account_id " +
		"where s.user_id=:userId " +
		"order by created_at"
	)
  @UseRowMapper(DBSessionMapper.class)
  List<ForDatabase> fetchSessionListById(@Bind("userId") Long userId);
  
  @SqlQuery("select * from member where user_id=:userId order by role, created_at")
  @UseRowMapper(MemberMapper.class)
  List<Member> fetchMembershipListById(@Bind("userId") Long userId);

	@SqlUpdate("update user set banned=true, ban_reason=:reason, banned_at=now() where id=:id")
	boolean ban(@Bind("id") Long id, @Bind("reason") String reason);

	@SqlUpdate("update user set banned=false, ban_reason=null, banned_at=null where id=:id")
	boolean revokeBan(@Bind("id") Long id);

}
