package io.inprice.api.app.link;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.models.Link;

interface LinkDao {

  @SqlQuery("select * from link where id=:id")
  @UseRowMapper(LinkMapper.class)
  Link findById(@Bind("id") Long id);

  @SqlQuery("select * from link where url_hash=:urlHash limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findByUrlHash(@Bind("urlHash") String urlHash);

  @SqlQuery("select * from link where product_id=:productId and url_hash=:urlHash")
  @UseRowMapper(LinkMapper.class)
  Link findByProductIdAndUrlHash(@Bind("productId") Long productId, @Bind("urlHash") String urlHash);

  @SqlUpdate(
    "insert into link (url, url_hash, product_id, company_id) " +
    "values (:url, :urlHash, :productId, :companyId)"
  )
  long insert(@Bind("url") String url, @Bind("urlHash") String urlHash, 
    @Bind("productId") Long productId, @Bind("companyId") Long companyId);

  @SqlUpdate(
    "insert into link (url, url_hash, sku, name, brand, seller, shipment, status, http_status, website_class_name, site_id, product_id, company_id) " +
    "values (:link.url, :link.urlHash, :link.sku, :link.name, :link.brand, :link.seller, :link.shipment, :link.status, :link.httpStatus, " +
      ":link.websiteClassName, :link.siteId, :productId, :companyId)"
  )
  long insert(@BindBean("link") Link sample, @Bind("productId") Long productId, @Bind("companyId") Long companyId);

  @SqlUpdate(
    "update link " + 
    "set pre_status=status, status=:newStatus, last_update=now() " + 
    "where id=:id " + 
    "  and status != :newStatus " + 
    "  and company_id=:companyId "
  )
  boolean changeStatus(@Bind("id") Long id, @Bind("newStatus") String newStatus, @Bind("companyId") Long companyId);

  @SqlUpdate(
    "insert into link_history (link_id, status, product_id, company_id) " +
    "values (:link_id, :status, :productId, :companyId)"
  )
  boolean insertLinkHistory(@Bind("linkId") Long linkId, @Bind("status") String status, @Bind("productId") Long productId, @Bind("companyId") Long companyId);

}
