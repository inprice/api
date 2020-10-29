package io.inprice.api.app.link;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.LinkHistoryMapper;
import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.mappers.LinkPriceMapper;
import io.inprice.common.mappers.LinkSpecMapper;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.models.LinkPrice;
import io.inprice.common.models.LinkSpec;

public interface LinkDao {

  @SqlQuery("select * from link where id=:id")
  @UseRowMapper(LinkMapper.class)
  Link findById(@Bind("id") Long id);

  @SqlQuery("select * from link where url_hash=:urlHash and (status=:status or pre_status=:status) limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findSampleByUrlHashAndStatus(@Bind("urlHash") String urlHash, @Bind("status") String status);

  @SqlQuery("select * from link where product_id is null and url_hash=:urlHash and company_id=:companyId limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findByUrlHashForImport(@Bind("urlHash") String urlHash, @Bind("companyId") Long companyId);

  @SqlQuery("select * from link where product_id=:productId and url_hash=:urlHash limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findByProductIdAndUrlHash(@Bind("productId") Long productId, @Bind("urlHash") String urlHash);

  @SqlQuery(
    "select l.*, s.name as platform, p.price as product_price from link as l " + 
    "inner join product as p on p.id = l.product_id " + 
    "left join site as s on s.id = l.site_id " + 
    "where l.product_id=:productId " +
    "  and l.company_id=:companyId " +
    "order by l.status, l.seller"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findListByProductIdAndCompanyId(@Bind("productId") Long productId, @Bind("companyId") Long companyId);

  @SqlUpdate(
    "insert into link (url, url_hash, product_id, company_id) " +
    "values (:url, :urlHash, :productId, :companyId)"
  )
  @GetGeneratedKeys
  long insert(@Bind("url") String url, @Bind("urlHash") String urlHash, @Bind("productId") Long productId, @Bind("companyId") Long companyId);

  @SqlUpdate(
    "insert into link (url, url_hash, sku, name, brand, seller, shipment, status, http_status, website_class_name, site_id, product_id, company_id) " +
    "values (:link.url, :link.urlHash, :link.sku, :link.name, :link.brand, :link.seller, :link.shipment, :link.status, :link.httpStatus, " +
      ":link.websiteClassName, :link.siteId, :productId, :companyId)"
  )
  @GetGeneratedKeys
  long insert(@BindBean("link") Link sample, @Bind("productId") Long productId, @Bind("companyId") Long companyId);

  @SqlQuery("select * from link_spec where product_id=:productId order by link_id, _key")
  @UseRowMapper(LinkSpecMapper.class)
  List<LinkSpec> findSpecListByProductId(@Bind("productId") Long productId);

  @SqlQuery("select * from link_price where product_id=:productId order by link_id, created_at desc")
  @UseRowMapper(LinkPriceMapper.class)
  List<LinkPrice> findPriceListByProductId(@Bind("productId") Long productId);

  @SqlQuery("select * from link_history where product_id=:productId order by link_id, created_at desc")
  @UseRowMapper(LinkHistoryMapper.class)
  List<LinkHistory> findHistoryListByProductId(@Bind("productId") Long productId);

}
