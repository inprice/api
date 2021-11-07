package io.inprice.api.app.exim.product;

import java.util.HashMap;
import java.util.List;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import io.inprice.api.dto.ProductDTO;

public interface ProductDao {

	@SqlQuery("select exists(select 1 from product where sku=:sku and workspace_id=:workspaceId)")
	boolean doesSkuExist(@Bind("sku") String sku, @Bind("workspaceId") Long workspaceId);

  @SqlBatch(
		"insert into product (sku, name, price, brand_id, category_id, workspace_id) " +
  	"values (:prod.sku, :prod.name, :prod.price, :prod.brandId, :prod.categoryId, :prod.workspaceId)"
	)
  void insertAll(@BindBean("prod") List<ProductDTO> prodList);	

  @SqlUpdate("insert into brand (name, workspace_id) values (:name, :workspaceId)")
  @GetGeneratedKeys()
  long insertBrand(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select lower(name) as name, id from brand where workspace_id=:workspaceId")
  @KeyColumn("name")
  @ValueColumn("id")
  HashMap<String, Long> getBrands(@Bind("workspaceId") Long workspaceId);

  @SqlUpdate("insert into category (name, workspace_id) values (:name, :workspaceId)")
  @GetGeneratedKeys()
  long insertCategory(@Bind("name") String name, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select lower(name) as name, id from category where workspace_id=:workspaceId")
  @KeyColumn("name")
  @ValueColumn("id")
  HashMap<String, Long> getCategories(@Bind("workspaceId") Long workspaceId);

}
