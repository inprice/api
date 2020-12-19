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

  @SqlQuery("select distinct name from product_tag where account_id=:accountId order by name")
  @UseRowMapper(ProductTagMapper.class)
  List<String> findAll(@Bind("accountId") Long accountId);

  @SqlUpdate("delete from product_tag where product_id=:productId and account_id=:accountId")
  void deleteTags(@Bind("productId") Long productId, @Bind("accountId") Long accountId);

  @SqlBatch("insert into product_tag (product_id, account_id, name) values (:productId, :accountId, :tags)")
  void insertTags(@Bind("productId") Long productId, @Bind("accountId") Long accountId, @Bind("tags") Set<String> tags);

}
