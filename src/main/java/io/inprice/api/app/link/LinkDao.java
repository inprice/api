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

  @SqlQuery("select * from link where url_hash=:urlHash and status=:status or pre_status=:status) limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findSampleByUrlHashAndStatus(@Bind("urlHash") String urlHash, @Bind("status") String status);

  @SqlQuery("select * from link where product_id is null and url_hash=:urlHash order by status, last_check desc")
  @UseRowMapper(LinkMapper.class)
  List<Link> findByUrlHashForImport(@Bind("urlHash") String urlHash);

  @SqlQuery("select * from link where product_id=:productId and url_hash=:urlHash limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findByProductIdAndUrlHash(@Bind("productId") Long productId, @Bind("urlHash") String urlHash);

  @SqlQuery(
    "select l.*, p.price as product_price from link as l " + 
    "inner join product as p on p.id = l.product_id " + 
    "where l.product_id=:productId " +
    "  and l.account_id=:accountId " +
    "order by l.id"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findListByProductIdAndAccountId(@Bind("productId") Long productId, @Bind("accountId") Long accountId);

  @SqlUpdate(
    "insert into link (url, url_hash, product_id, account_id) " +
    "values (:url, :urlHash, :productId, :accountId)"
  )
  @GetGeneratedKeys
  long insert(@Bind("url") String url, @Bind("urlHash") String urlHash, @Bind("productId") Long productId, @Bind("accountId") Long accountId);

  @SqlUpdate(
    "insert into link (url, url_hash, sku, name, brand, seller, shipment, status, last_http_status, platform_id, product_id, account_id) " +
    "values (:link.url, :link.urlHash, :link.sku, :link.name, :link.brand, :link.seller, :link.shipment, :link.status, :link.lastHttpStatus, " +
      ":link.platformId, :productId, :accountId)"
  )
  @GetGeneratedKeys
  long insert(@BindBean("link") Link sample, @Bind("productId") Long productId, @Bind("accountId") Long accountId);

  @SqlQuery("select * from link_history where product_id=:productId order by link_id, id desc")
  @UseRowMapper(LinkHistoryMapper.class)
  List<LinkHistory> findHistoryListByProductId(@Bind("productId") Long productId);

  @SqlQuery("select * from link_price where product_id=:productId order by link_id, id desc")
  @UseRowMapper(LinkPriceMapper.class)
  List<LinkPrice> findPriceListByProductId(@Bind("productId") Long productId);

  @SqlQuery("select * from link_spec where product_id=:productId order by link_id, _key")
  @UseRowMapper(LinkSpecMapper.class)
  List<LinkSpec> findSpecListByProductId(@Bind("productId") Long productId);

  @SqlQuery("select * from link_history where link_id=:linkId order by id desc")
  @UseRowMapper(LinkHistoryMapper.class)
  List<LinkHistory> findHistoryListByLinkId(@Bind("linkId") Long linkId);

  @SqlQuery("select * from link_history where link_id=:linkId order by id desc limit 3")
  @UseRowMapper(LinkHistoryMapper.class)
  List<LinkHistory> findLastThreeHistoryRowsByLinkId(@Bind("linkId") Long linkId);

  @SqlQuery("select * from link_price where link_id=:linkId order by id desc")
  @UseRowMapper(LinkPriceMapper.class)
  List<LinkPrice> findPriceListByLinkId(@Bind("linkId") Long linkId);

  @SqlQuery("select * from link_spec where link_id=:linkId order by _key")
  @UseRowMapper(LinkSpecMapper.class)
  List<LinkSpec> findSpecListByLinkId(@Bind("linkId") Long linkId);

  @SqlUpdate(
    "insert into link_history (link_id, status, last_http_status, last_problem, product_id, account_id) " +
    "values (:link.id, :link.status, :link.lastHttpStatus, :link.lastProblem, :link.productId, :link.accountId)"
  )
  @GetGeneratedKeys
  long insertHistory(@BindBean("link") Link link);

  @SqlUpdate("update link set pre_status=status, status=:status, last_update=now() where id=:id")
  boolean toggleStatus(@Bind("id") Long id, @Bind("status") String status);

  @SqlUpdate(
    "insert into link (url, url_hash, status, problem, retry, platform_id, import_detail_id, account_id) " +
    "values (:url, :urlHash, :status, :problem, :retry, :platformId, :importDetailId, :accountId)"
  )
  @GetGeneratedKeys
  long importProduct(@Bind("url") String url, @Bind("urlHash") String urlHash, @Bind("status") String status, 
    @Bind("problem") String problem, @Bind("retry") int retry, @Bind("platformId") Long platformId, 
    @Bind("importDetailId") Long importDetailId, @Bind("accountId") Long accountId);

}
