package io.inprice.api.app.group;

import java.math.BigDecimal;
import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.LinkGroupMapper;
import io.inprice.common.models.LinkGroup;

public interface GroupDao {

	@SqlQuery("select * from link_group where id=:id and account_id=:accountId")
	@UseRowMapper(LinkGroupMapper.class)
	LinkGroup findById(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlQuery("select * from link_group where name=:name and account_id=:accountId")
  @UseRowMapper(LinkGroupMapper.class)
  LinkGroup findByName(@Bind("name") String name, @Bind("accountId") Long accountId);
  
  @SqlQuery("select * from link_group where id!=:excludedId and account_id = :accountId order by name")
  @UseRowMapper(LinkGroupMapper.class)
  List<LinkGroup> getList(@Bind("excludedId") Long excludedId, @Bind("accountId") Long accountId);

  @SqlQuery("select * from link_group where name like :term and account_id = :accountId order by name")
  @UseRowMapper(LinkGroupMapper.class)
  List<LinkGroup> search(@Bind("term") String term, @Bind("accountId") Long accountId);

  @SqlUpdate("insert into link_group (name, price, account_id) values (:name, :price, :accountId)")
  @GetGeneratedKeys()
  long insert(@Bind("name") String name, @Bind("price") BigDecimal price, @Bind("accountId") Long accountId);

  @SqlUpdate("update link_group set name=:name, price=:price where id=:id and account_id=:accountId")
  boolean update(@Bind("id") Long id, @Bind("name") String name, @Bind("price") BigDecimal price, @Bind("accountId") Long accountId);

  @SqlUpdate("delete from link_group where id=:groupId and account_id=:accountId")
  boolean delete(@Bind("groupId") Long id, @Bind("accountId") Long accountId);

  //called after bulkInsert
  @SqlUpdate("update link_group set waitings=waitings+:waitings where id=:id")
  boolean increaseWaitingsCount(@Bind("id") Long id, @Bind("waitings") Integer waitings);

}
