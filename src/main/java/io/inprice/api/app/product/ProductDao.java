package io.inprice.api.app.product;

import java.util.List;
import java.util.Set;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.brand.BrandDao;
import io.inprice.api.app.category.CategoryDao;
import io.inprice.api.dto.ProductDTO;
import io.inprice.common.mappers.IdNamePairMapper;
import io.inprice.common.mappers.ProductMapper;
import io.inprice.common.models.Product;

public interface ProductDao {

	@SqlQuery("select * from product where id=:id and workspace_id=:workspaceId")
	@UseRowMapper(ProductMapper.class)
	Product findById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select exists(" +
			"select 1 from product " +
			"where sku = :dto.sku " +
			"  and id != :dto.id " +
			"  and workspace_id = :workspaceId " +
		")")
	boolean doesExistBySku(@BindBean("dto") ProductDTO dto, @Bind("workspaceId") Long workspaceId);

	@SqlQuery(
		"select p.*, al.name as al_name, sp.name as sp_name" + BrandDao.FIELDS + CategoryDao.FIELDS + " from product p " +
		"left join alarm as al on al.id = p.alarm_id " +
		"left join smart_price as sp on sp.id = p.smart_price_id " +
		"left join brand as brn on brn.id = p.brand_id " +
		"left join category as cat on cat.id = p.category_id " +
		"where p.id=:id " +
		"  and p.workspace_id=:workspaceId"
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
		"insert into product (sku, name, price, base_price, brand_id, category_id, alarm_id, smart_price_id, workspace_id) " +
		"values (:dto.sku, :dto.name, :dto.price, :dto.basePrice, :dto.brandId, :dto.categoryId, :dto.alarmId, :dto.smartPriceId, :dto.workspaceId)"
	)
  @GetGeneratedKeys()
  long insert(@BindBean("dto") ProductDTO dto);

  //called after adding links
  @SqlUpdate("update product set waitings=waitings + <count> where id=:id")
  boolean incWaitingsCount(@Bind("id") Long id, @Define("count") Integer count);

  @SqlUpdate(
		"update product set alarm_id=:alarmId, tobe_alarmed=false, alarmed_at=null " +
		"where id in (<productIdSet>) " +
		"  and workspace_id = :workspaceId"
	)
  int setAlarmON(@Bind("alarmId") Long alarmId, @BindList("productIdSet") Set<Long> productIdSet, @Bind("workspaceId") Long workspaceId);

  @SqlUpdate(
		"update product set alarm_id=null, tobe_alarmed=false, alarmed_at=null " +
		"where id in (<productIdSet>) " +
		"  and workspace_id = :workspaceId"
	)
  int setAlarmOFF(@BindList("productIdSet") Set<Long> productIdSet, @Bind("workspaceId") Long workspaceId);

}
