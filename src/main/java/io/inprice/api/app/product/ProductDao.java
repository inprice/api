package io.inprice.api.app.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import io.inprice.api.app.product.mapper.ProductReducer;
import io.inprice.api.app.product.mapper.SimpleSearch;
import io.inprice.api.app.product.mapper.SimpleSearchMapper;
import io.inprice.common.mappers.ProductMapper;
import io.inprice.common.models.Product;
import io.inprice.common.models.ProductTag;

public interface ProductDao {

  @SqlQuery(
    "select "+PRODUCT_FIELDS+", "+TAG_FIELDS+" from product as p " +
    "left join product_tag as pt on pt.product_id = p.id " +
    "where p.id=:id and p.account_id=:accountId"
  )
  @RegisterBeanMapper(value = Product.class, prefix = "p")
  @RegisterBeanMapper(value = ProductTag.class, prefix = "pt")
  @UseRowReducer(ProductReducer.class)
  Product findById(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlQuery("select * from product where code=:code and account_id=:accountId")
  @UseRowMapper(ProductMapper.class)
  Product findByCode(@Bind("code") String code, @Bind("accountId") Long accountId);

  @SqlQuery(
    "select id, code, name from product " +
    "where account_id=:accountId and (code like '%' || :term || '%' or name like '%' || :term || '%') order by name limit <limit>"
  )
  @UseRowMapper(SimpleSearchMapper.class)
  List<SimpleSearch> searchSimpleByTermAndAccountId(@Bind("term") String term, @Bind("accountId") Long accountId, @Define("limit") int limit);

  @SqlUpdate("insert into product (code, name, price, account_id) values (:code, :name, :price, :accountId)")
  @GetGeneratedKeys()
  long insert(@Bind("code") String code, @Bind("name") String name, @Bind("price") BigDecimal price, @Bind("accountId") Long accountId);

  @SqlUpdate(
    "update product set code=:code, name=:name, price=:price " +
    "where id=:id and account_id=:accountId"
  )
  boolean update(@Bind("id") Long id, @Bind("accountId") Long accountId,
    @Bind("code") String code, @Bind("name") String name, @Bind("price") BigDecimal price);

  @SqlQuery(
    "select position, count(1) as counter from product " +
    "where account_id=:accountId " +
    "group by position "
  )
  @KeyColumn("position")
  @ValueColumn("counter")
  Map<Integer, Integer> findPositionDists(@Bind("accountId") Long accountId);

  //look at ProductReducer class
  //these are necessary mappings since we need to establish one-to-one and one-to-many relations between tables
  final String PRODUCT_FIELDS = 
    "p.id as p_id, " +
    "p.code as p_code, " +
    "p.name as p_name, " +
    "p.price as p_price, " +
    "p.position as p_position, " +
    "p.link_count as p_link_count, " +
    "p.ranking as p_ranking, " +
    "p.ranking_with as p_ranking_with, " +
    "p.min_platform as p_min_platform, " +
    "p.min_seller as p_min_seller, " +
    "p.min_price as p_min_price, " +
    "p.min_diff as p_min_diff, " +
    "p.avg_price as p_avg_price, " +
    "p.avg_diff as p_avg_diff, " +
    "p.max_platform as p_max_platform, " +
    "p.max_seller as p_max_seller, " +
    "p.max_price as p_max_price, " +
    "p.max_diff as p_max_diff, " +
    "p.suggested_price as p_suggested_price, " +
    "p.updated_at as p_updated_at, " +
    "p.created_at as p_created_at, " +
    "p.account_id as p_account_id ";
    
  final String TAG_FIELDS = 
    "pt.id as pt_id, " +
    "pt.name as pt_name, " +
    "pt.product_id as pt_product_id, " +
    "pt.account_id as pt_account_id ";

}
