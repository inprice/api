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

	@SqlQuery("select * from product where id=:id and workspace_id=:workspaceId")
	@UseRowMapper(ProductMapper.class)
	Product findById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

	@SqlQuery("select * from product where name=:name and id!=:id and workspace_id=:workspaceId limit 1")
	@UseRowMapper(ProductMapper.class)
	Product findByName(@Bind("name") String name, @Bind("id") Long otherThanThisId, @Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select g.*" + AlarmDao.FIELDS + " from product g " +
		"left join alarm as al on al.id=g.alarm_id " +
		"where g.id=:id " +
		"  and g.workspace_id=:workspaceId"
	)
	@UseRowMapper(ProductMapper.class)
	Product findByIdWithAlarm(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from product where code=:code and workspace_id=:workspaceId")
  @UseRowMapper(ProductMapper.class)
  Product findByCode(@Bind("code") String code, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from product where name=:name and workspace_id=:workspaceId limit 1")
  @UseRowMapper(ProductMapper.class)
	Product findByName(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select id, name from product where id!=:excludedId and workspace_id = :workspaceId order by name")
  @UseRowMapper(IdNamePairMapper.class)
  List<IdNamePairMapper> getIdNameList(@Bind("excludedId") Long excludedId, @Bind("workspaceId") Long workspaceId);

  @SqlQuery(
		"select g.*" + AlarmDao.FIELDS + " from product as g " +
		"left join alarm as al on al.id = g.alarm_id " +
		"where g.name like :dto.term " +
		"  and g.workspace_id = :dto.workspaceId " +
		"order by g.name " +
		"limit :dto.rowCount, :dto.rowLimit "
	)
  @UseRowMapper(ProductMapper.class)
	List<Product> search(@BindBean("dto") BaseSearchDTO dto);

  @SqlUpdate(
		"insert into product (code, name, description, price, brand_id, category_id, workspace_id) " +
		"values (:dto.code, :dto.name, :dto.description, :dto.price, :dto.brandId, :dto.categoryId, :dto.workspaceId)"
	)
  @GetGeneratedKeys()
  long insert(@BindBean("dto") ProductDTO dto);

  @SqlUpdate(
		"update product " +
		"set name=:dto.name, description=:dto.description, price=:dto.price, brand_id=:dto.brandId, category_id=:dto.categoryId " +
		"where id=:dto.id " +
		"  and workspace_id=:dto.workspaceId"
	)
  boolean update(@BindBean("dto") ProductDTO dto);

  //called after adding links
  @SqlUpdate("update product set waitings=waitings + <count> where id=:id")
  boolean increaseWaitingsCount(@Bind("id") Long id, @Define("count") Integer count);

}
