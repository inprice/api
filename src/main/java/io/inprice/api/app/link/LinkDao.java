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
  Link findById(@Bind Long id);

  @SqlQuery("select * from link where url_hash=:urlHash limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findByUrlHash(@Bind String urlHash);

  @SqlQuery("select * from link where product_id=:productId and url_hash=:urlHash")
  @UseRowMapper(LinkMapper.class)
  Link findByProductIdAndUrlHash(@Bind Long productId, @Bind String urlHash);

  @SqlUpdate(
    "insert into link (url, url_hash, product_id, company_id) " +
    "values (:url, :urlHash, :productId, :companyId)"
  )
  long insertLink(@Bind String url, @Bind String urlHash, @Bind Long productId, @Bind Long companyId);

  @SqlUpdate(
    "insert into link (url, url_hash, sku, name, brand, seller, shipment, status, http_status, website_class_name, site_id, product_id, company_id) " +
    "values (:link.url, :link.urlHash, :link.sku, :link.name, :link.brand, :link.seller, :link.shipment, :link.status, :link.httpStatus, " +
      ":link.websiteClassName, :link.siteId, :productId, :companyId)"
  )
  long insertLink(@BindBean("link") Link sample, @Bind Long productId, @Bind Long companyId);

  @SqlUpdate(
    "update link " + 
    "set pre_status=status, status=:newStatus, last_update=now() " + 
    "where id=:id " + 
    "  and status != :newStatus " + 
    "  and company_id=:companyId "
  )
  boolean changeStatus(@Bind Long id, @Bind String newStatus, @Bind Long companyId);

  @SqlUpdate(
    "insert into link_history (link_id, status, product_id, company_id) " +
    "values (:link_id, :status, :productId, :companyId)"
  )
  boolean insertLinkHistory(@Bind Long linkId, @Bind String status, @Bind Long productId, @Bind Long companyId);

}
