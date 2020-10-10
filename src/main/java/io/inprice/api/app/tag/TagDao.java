package io.inprice.api.app.tag;

import java.util.List;
import java.util.Set;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.ProductTagMapper;

public interface TagDao {

  @SqlQuery("select distinct name from product_tag where company_id=:companyId order by name")
  @UseRowMapper(ProductTagMapper.class)
  List<String> findAll(@Bind("companyId") Long companyId);

  @SqlUpdate("delete from product_tag where product_id=:productId and company_id=:companyId")
  void deleteTags(@Bind("productId") Long productId, @Bind("companyId") Long companyId);

  @SqlBatch("insert into product_tag (product_id, company_id, name) values (:productId, :companyId, :tags)")
  void insertTags(@Bind("productId") Long productId, @Bind("companyId") Long companyId, @Bind("tags") Set<String> tags);

}
