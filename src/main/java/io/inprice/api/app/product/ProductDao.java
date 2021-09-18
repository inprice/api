package io.inprice.api.app.product;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.ProductDTO;
import io.inprice.common.mappers.IdNamePairMapper;
import io.inprice.common.mappers.ProductMapper;
import io.inprice.common.models.Product;
import io.inprice.common.repository.AlarmDao;

public interface ProductDao {

	@SqlQuery("select * from product where id=:id and account_id=:accountId")
	@UseRowMapper(ProductMapper.class)
	Product findById(@Bind("id") Long id, @Bind("accountId") Long accountId);

	@SqlQuery("select * from product where name=:name and id!=:id and account_id=:accountId limit 1")
	@UseRowMapper(ProductMapper.class)
	Product findByName(@Bind("name") String name, @Bind("id") Long otherThanThisId, @Bind("accountId") Long accountId);

	@SqlQuery(
		"select g.*" + AlarmDao.FIELDS + " from product g " +
		"left join alarm as al on al.id=g.alarm_id " +
		"where g.id=:id " +
		"  and g.account_id=:accountId"
	)
	@UseRowMapper(ProductMapper.class)
	Product findByIdWithAlarm(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlQuery("select * from product where name=:name and account_id=:accountId")
  @UseRowMapper(ProductMapper.class)
  Product findByName(@Bind("name") String name, @Bind("accountId") Long accountId);

  @SqlQuery("select id, name from product where id!=:excludedId and account_id = :accountId order by name")
  @UseRowMapper(IdNamePairMapper.class)
  List<IdNamePairMapper> getIdNameList(@Bind("excludedId") Long excludedId, @Bind("accountId") Long accountId);

  @SqlQuery(
		"select g.*" + AlarmDao.FIELDS + " from product as g " +
		"left join alarm as al on al.id = g.alarm_id " +
		"where g.name like :dto.term " +
		"  and g.account_id = :dto.accountId " +
		"order by g.name " +
		"limit :dto.rowCount, :dto.rowLimit "
	)
  @UseRowMapper(ProductMapper.class)
	List<Product> search(@BindBean("dto") BaseSearchDTO dto);

  @SqlUpdate("insert into product (name, description, price, account_id) values (:dto.name, :dto.description, :dto.price, :dto.accountId)")
  @GetGeneratedKeys()
  long insert(@BindBean("dto") ProductDTO dto);

  @SqlUpdate("update product set name=:dto.name, description=:dto.description, price=:dto.price where id=:dto.id and account_id=:dto.accountId")
  boolean update(@BindBean("dto") ProductDTO dto);

  //called after adding links
  @SqlUpdate("update product set waitings=waitings + <count> where id=:id")
  boolean increaseWaitingsCount(@Bind("id") Long id, @Define("count") Integer count);

}
