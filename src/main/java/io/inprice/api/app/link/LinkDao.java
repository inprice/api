package io.inprice.api.app.link;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.BindList;
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
import io.inprice.common.repository.AlarmDao;
import io.inprice.common.repository.PlatformDao;

public interface LinkDao {

  @SqlQuery("select * from link where id=:id and workspace_id=:workspaceId")
  @UseRowMapper(LinkMapper.class)
  Link findById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery(
		"select *" + AlarmDao.FIELDS + " from link as l " +
    "left join alarm as al on al.id = l.alarm_id " + 
    "where l.id=:id " +
    "  and l.workspace_id=:workspaceId"
	)
  @UseRowMapper(LinkMapper.class)
  Link findWithAlarmById(@Bind("id") Long id, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from link where product_id=:productId and url_hash=:urlHash limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findByProductIdAndUrlHash(@Bind("productId") Long productId, @Bind("urlHash") String urlHash);

  @SqlQuery(
    "select l.*" + PlatformDao.FIELDS + AlarmDao.FIELDS + ", g.price as product_price from link as l " + 
		"inner join product as g on g.id = l.product_id " + 
		"left join platform as p on p.id = l.platform_id " + 
    "left join alarm as al on al.id = l.alarm_id " + 
    "where l.product_id=:productId " +
    "  and l.workspace_id=:workspaceId " +
    "order by l.grup, l.level, l.price"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findListByProductId(@Bind("productId") Long productId, @Bind("workspaceId") Long workspaceId);

  @SqlQuery("select * from link_price where product_id=:productId order by link_id, id desc")
  @UseRowMapper(LinkPriceMapper.class)
  List<LinkPrice> findPriceListByProductId(@Bind("productId") Long productId);

  @SqlQuery("select * from link_spec where product_id=:productId order by link_id, _key")
  @UseRowMapper(LinkSpecMapper.class)
  List<LinkSpec> findSpecListByProductId(@Bind("productId") Long productId);

  @SqlQuery("select * from link_history where product_id=:productId order by link_id, id desc")
  @UseRowMapper(LinkHistoryMapper.class)
  List<LinkHistory> findHistoryListByProductId(@Bind("productId") Long productId);

  @SqlQuery("select * from link_history where link_id=:linkId order by id desc limit 20")
  @UseRowMapper(LinkHistoryMapper.class)
  List<LinkHistory> findHistoryListByLinkId(@Bind("linkId") Long linkId);

  @SqlQuery("select * from link_price where link_id=:linkId order by id desc limit 20")
  @UseRowMapper(LinkPriceMapper.class)
  List<LinkPrice> findPriceListByLinkId(@Bind("linkId") Long linkId);

  @SqlQuery("select * from link_spec where link_id=:linkId order by _key limit 30")
  @UseRowMapper(LinkSpecMapper.class)
  List<LinkSpec> findSpecListByLinkId(@Bind("linkId") Long linkId);

  @SqlUpdate(
    "insert into link (url, url_hash, product_id, workspace_id) " +
    "values (:link.url, :link.urlHash, :link.productId, :link.workspaceId)"
  )
  @GetGeneratedKeys
  long insert(@BindBean("link") Link sample);

  @SqlUpdate(
    "insert into link_history (link_id, status, product_id, workspace_id) " +
    "values (:link.id, :link.status, :link.productId, :link.workspaceId)"
  )
  @GetGeneratedKeys
  long insertHistory(@BindBean("link") Link link);

  @SqlQuery("select product_id from link where id in (<linkIdSet>)")
  HashSet<Long> findProductIdSet(@BindList("linkIdSet") Set<Long> linkIdSet);

  @SqlQuery("select grup from link where id = :et")
  String findProductIdSet__(@Bind("et") Long et);

}
