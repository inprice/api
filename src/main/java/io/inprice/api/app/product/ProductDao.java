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
import io.inprice.common.models.ProductPrice;
import io.inprice.common.models.ProductTag;

public interface ProductDao {

  @SqlQuery(
    "select "+PRODUCT_FIELDS+", "+PRICE_FIELDS+", "+TAG_FIELDS+" from product as p " +
    "left join product_price as pp on pp.id = p.last_price_id " +
    "left join product_tag as pt on pt.product_id = p.id " +
    "where p.id=:id and p.company_id=:companyId"
  )
  @RegisterBeanMapper(value = Product.class, prefix = "p")
  @RegisterBeanMapper(value = ProductPrice.class, prefix = "pp")
  @RegisterBeanMapper(value = ProductTag.class, prefix = "pt")
  @UseRowReducer(ProductReducer.class)
  Product findById(@Bind("id") Long id, @Bind("companyId") Long companyId);

  @SqlQuery("select * from product where code=:code and company_id=:companyId")
  @UseRowMapper(ProductMapper.class)
  Product findByCode(@Bind("code") String code, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select id, code, name from product " +
    "where company_id=:companyId and (code like '%' || :term || '%' or name like '%' || :term || '%') order by name limit <limit>"
  )
  @UseRowMapper(SimpleSearchMapper.class)
  List<SimpleSearch> searchSimpleByTermAndCompanyId(@Bind("term") String term, @Bind("companyId") Long companyId, @Define("limit") int limit);

  @SqlUpdate("insert into product (code, name, price, company_id) values (:code, :name, :price, :companyId)")
  @GetGeneratedKeys()
  long insert(@Bind("code") String code, @Bind("name") String name, @Bind("price") BigDecimal price, @Bind("companyId") Long companyId);

  @SqlUpdate(
    "update product set code=:code, name=:name, price=:price " +
    "where id=:id and company_id=:companyId"
  )
  boolean update(@Bind("id") Long id, @Bind("companyId") Long companyId,
    @Bind("code") String code, @Bind("name") String name, @Bind("price") BigDecimal price);

  @SqlUpdate("update product set active = not active where id=:id and company_id=:companyId")
  boolean toggleStatus(@Bind("id") Long id, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select position, count(1) as counter from product " +
    "where company_id=:companyId " +
    "group by position "
  )
  @KeyColumn("position")
  @ValueColumn("counter")
  Map<Integer, Integer> findPositionDists(@Bind("companyId") Long companyId);

  //look at ProductReducer class
  //these are necessary mappings since we need to establish one-to-one and one-to-many relations between tables
  final String PRODUCT_FIELDS = 
    "p.id as p_id, " +
    "p.active as p_active, " +
    "p.code as p_code, " +
    "p.name as p_name, " +
    "p.price as p_price, " +
    "p.position as p_position, " +
    "p.last_price_id as p_last_price_id, " +
    "p.updated_at as p_updated_at, " +
    "p.created_at as p_created_at, " +
    "p.company_id as p_company_id ";
    
  final String PRICE_FIELDS = 
    "pp.id as pp_id, " +
    "pp.product_id as pp_product_id, " +
    "pp.price as pp_price, " +
    "pp.min_platform as pp_min_platform, " +
    "pp.min_seller as pp_min_seller, " +
    "pp.min_price as pp_min_price, " +
    "pp.min_diff as pp_min_diff, " +
    "pp.avg_price as pp_avg_price, " +
    "pp.avg_diff as pp_avg_diff, " +
    "pp.max_platform as pp_max_platform, " +
    "pp.max_seller as pp_max_seller, " +
    "pp.max_price as pp_max_price, " +
    "pp.max_diff as pp_max_diff, " +
    "pp.links as pp_links, " +
    "pp.position as pp_position, " +
    "pp.ranking as pp_ranking, " +
    "pp.ranking_with as pp_ranking_with, " +
    "pp.suggested_price as pp_suggested_price, " +
    "pp.created_at as pp_created_at, " +
    "pp.product_id as pp_product_id, " +
    "pp.company_id as pp_company_id ";
  
  final String TAG_FIELDS = 
    "pt.id as pt_id, " +
    "pt.name as pt_name, " +
    "pt.product_id as pt_product_id, " +
    "pt.company_id as pt_company_id ";

}
