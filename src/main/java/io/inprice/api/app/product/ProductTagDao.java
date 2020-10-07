package io.inprice.api.app.product;

import java.util.List;
import java.util.Set;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.ProductTagMapper;
import io.inprice.common.models.ProductTag;

public interface ProductTagDao {

  @SqlQuery("select distinct name from product_tag where company_id=:companyId order by name")
  @UseRowMapper(ProductTagMapper.class)
  List<ProductTag> findAll(@Bind("companyId") Long companyId);

  @SqlQuery("select distinct name from product_tag where product_id=:productId and company_id=:companyId order by name")
  @UseRowMapper(ProductTagMapper.class)
  List<ProductTag> findListByProductId(@Bind("productId") Long productId, @Bind("companyId") Long companyId);

  @SqlBatch("delete from product_tag where product_id=:productId and company_id=:companyId")
  void deleteTags(@Bind("productId") Long productId, @Bind("companyId") Long companyId);

  @SqlBatch("insert into product_tag (name, product_id, company_id) values (?, ?, ?)")
  void insertTags(Long productId, Long companyId, Set<String> tags);

}
