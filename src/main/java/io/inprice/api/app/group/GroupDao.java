package io.inprice.api.app.group;

import java.math.BigDecimal;
import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.common.mappers.IdNamePairMapper;
import io.inprice.common.mappers.LinkGroupMapper;
import io.inprice.common.models.LinkGroup;
import io.inprice.common.repository.AlarmDao;

public interface GroupDao {

	@SqlQuery("select * from link_group where id=:id and account_id=:accountId")
	@UseRowMapper(LinkGroupMapper.class)
	LinkGroup findById(@Bind("id") Long id, @Bind("accountId") Long accountId);

	@SqlQuery(
		"select g.*" + AlarmDao.FIELDS + " from link_group g " +
		"left join alarm as al on al.id=g.alarm_id " +
		"where g.id=:id " +
		"  and g.account_id=:accountId"
	)
	@UseRowMapper(LinkGroupMapper.class)
	LinkGroup findByIdWithAlarm(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlQuery("select * from link_group where name=:name and account_id=:accountId")
  @UseRowMapper(LinkGroupMapper.class)
  LinkGroup findByName(@Bind("name") String name, @Bind("accountId") Long accountId);

  @SqlQuery("select id, name from link_group where id!=:excludedId and account_id = :accountId order by name")
  @UseRowMapper(IdNamePairMapper.class)
  List<IdNamePairMapper> getIdNameList(@Bind("excludedId") Long excludedId, @Bind("accountId") Long accountId);

  @SqlQuery(
		"select g.*" + AlarmDao.FIELDS + " from link_group as g " +
		"left join alarm as al on al.id = g.alarm_id " +
		"where g.name like :dto.term " +
		"  and g.account_id = :dto.accountId " +
		"order by g.name " +
		"limit :dto.rowCount, :dto.rowLimit "
	)
  @UseRowMapper(LinkGroupMapper.class)
	List<LinkGroup> search(@BindBean("dto") BaseSearchDTO dto);

  @SqlUpdate("insert into link_group (name, price, account_id) values (:name, :price, :accountId)")
  @GetGeneratedKeys()
  long insert(@Bind("name") String name, @Bind("price") BigDecimal price, @Bind("accountId") Long accountId);

  @SqlUpdate("update link_group set name=:name, price=:price where id=:id and account_id=:accountId")
  boolean update(@Bind("id") Long id, @Bind("name") String name, @Bind("price") BigDecimal price, @Bind("accountId") Long accountId);

  //called after bulkInsert
  @SqlUpdate("update link_group set waitings=waitings + <count> where id=:id")
  boolean increaseWaitingsCount(@Bind("id") Long id, @Define("count") Integer count);

}
