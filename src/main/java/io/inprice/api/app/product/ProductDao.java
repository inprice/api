package io.inprice.api.app.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.api.app.product.mapper.SimpleSearch;
import io.inprice.api.app.product.mapper.SimpleSearchMapper;
import io.inprice.common.mappers.ProductMapper;
import io.inprice.common.models.Product;

public interface ProductDao {

  @SqlQuery("select * from product where code=:code and company_id=:companyId")
  @UseRowMapper(ProductMapper.class)
  Product findByCode(@Bind("code") String code, @Bind("companyId") Long companyId);
    
  @SqlQuery(
    "select p.*, pp.* from product as p " +
    "left join product_price as pp on p.last_price_id = pp.id " +
    "where p.id=:id and p.company_id=:companyId"
  )
  @UseRowMapper(ProductMapper.class)
  Product findByIdAndCompanyId(@Bind("id") Long id, @Bind("companyId") Long companyId);

  @SqlQuery(
    "select id, code, name from product " +
    "where company_id=:companyId and (code like '%' || :term || '%' or name like '%' || :term || '%') order by name limit <limit>"
  )
  @UseRowMapper(SimpleSearchMapper.class)
  List<SimpleSearch> searchSimpleByTermAndCompanyId(@Bind("term") String term, @Bind("companyId") Long companyId, @Define("limit") int limit);

  @SqlUpdate("insert into product (code, name, price, company_id) values (:code, :name, :price, :companyId)")
  boolean insert(@Bind("code") String code, @Bind("name") String name, @Bind("price") BigDecimal price, @Bind("companyId") Long companyId);

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

}
