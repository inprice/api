package io.inprice.api.app.product;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.definitions.brand.BrandDao;
import io.inprice.api.app.definitions.category.CategoryDao;
import io.inprice.api.dto.ProductDTO;
import io.inprice.common.mappers.IdNamePairMapper;
import io.inprice.common.mappers.ProductMapper;
import io.inprice.common.models.Product;
import io.inprice.common.repository.AlarmDao;

public interface ProductDao {

	@SqlQuery("select * from product where id=:id and workspace_id=:workspaceId")
	@UseRowMapper(ProductMapper.class)
	Product findById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select exists(" +
			"select 1 from product " +
			"where (sku = :dto.sku or name = :dto.name) " +
			"  and id != :dto.id " +
			"  and workspace_id = :workspaceId " +
		")")
	boolean doesExistBySkuAndName(@BindBean("dto") ProductDTO dto, @Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select exists(" +
			"select 1 from product " +
			"where name = :dto.name " +
			"  and id != :dto.id " +
			"  and workspace_id = :workspaceId " +
		")")
	boolean doesExistByName(@BindBean("dto") ProductDTO dto, @Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select g.*" + AlarmDao.FIELDS + BrandDao.FIELDS + CategoryDao.FIELDS + " from product g " +
		"left join alarm as al on al.id=g.alarm_id " +
		"left join brand as brn on brn.id=g.brand_id " +
		"left join category as cat on cat.id=g.category_id " +
		"where g.id=:id " +
		"  and g.workspace_id=:workspaceId"
	)
	@UseRowMapper(ProductMapper.class)
	Product findByIdWithLookups(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from product where name=:name and workspace_id=:workspaceId limit 1")
  @UseRowMapper(ProductMapper.class)
	Product findByName(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select id, name from product where id!=:excludedId and workspace_id = :workspaceId order by name")
  @UseRowMapper(IdNamePairMapper.class)
  List<IdNamePairMapper> getIdNameList(@Bind("excludedId") Long excludedId, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate(
		"insert into product (sku, name, price, brand_id, category_id, workspace_id) " +
		"values (:dto.sku, :dto.name, :dto.price, :dto.brandId, :dto.categoryId, :dto.workspaceId)"
	)
  @GetGeneratedKeys()
  long insert(@BindBean("dto") ProductDTO dto);

  @SqlUpdate(
		"update product " +
		"set sku=:dto.sku, name=:dto.name, price=:dto.price, brand_id=:dto.brandId, category_id=:dto.categoryId " +
		"where id=:dto.id " +
		"  and workspace_id=:dto.workspaceId"
	)
  boolean update(@BindBean("dto") ProductDTO dto);

  //called after adding links
  @SqlUpdate("update product set waitings=waitings + <count> where id=:id")
  boolean increaseWaitingsCount(@Bind("id") Long id, @Define("count") Integer count);

}
