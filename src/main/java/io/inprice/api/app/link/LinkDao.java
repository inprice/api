package io.inprice.api.app.link;

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
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.meta.LinkStatusGroup;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.models.LinkPrice;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.repository.AlarmDao;
import io.inprice.common.repository.PlatformDao;

public interface LinkDao {
	
  @SqlQuery("select * from link where id=:id and account_id=:accountId")
  @UseRowMapper(LinkMapper.class)
  Link findById(@Bind("id") Long id, @Bind("accountId") Long accountId);

  @SqlQuery("select * from link where url_hash=:urlHash and (status=:status or pre_status=:status) limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findSampleByUrlHashAndStatus(@Bind("urlHash") String urlHash, @Bind("status") LinkStatus status);

  @SqlQuery("select * from link where group_id=:groupId and url_hash=:urlHash limit 1")
  @UseRowMapper(LinkMapper.class)
  Link findByGroupIdAndUrlHash(@Bind("groupId") Long groupId, @Bind("urlHash") String urlHash);

  @SqlQuery(
    "select l.*" + PlatformDao.FIELDS + AlarmDao.FIELDS + ", g.price as group_price from link as l " + 
		"inner join link_group as g on g.id = l.group_id " + 
		"left join platform as p on p.id = l.platform_id " + 
    "left join alarm as al on al.id = l.alarm_id " + 
    "where l.group_id=:groupId " +
    "  and l.account_id=:accountId " +
    "order by l.status_group, l.level, l.price"
  )
  @UseRowMapper(LinkMapper.class)
  List<Link> findListByGroupId(@Bind("groupId") Long groupId, @Bind("accountId") Long accountId);

  @SqlQuery("select * from link_price where group_id=:groupId order by link_id, id desc")
  @UseRowMapper(LinkPriceMapper.class)
  List<LinkPrice> findPriceListByGroupId(@Bind("groupId") Long groupId);

  @SqlQuery("select * from link_spec where group_id=:groupId order by link_id, _key")
  @UseRowMapper(LinkSpecMapper.class)
  List<LinkSpec> findSpecListByGroupId(@Bind("groupId") Long groupId);

  @SqlQuery("select * from link_history where group_id=:groupId order by link_id, id desc")
  @UseRowMapper(LinkHistoryMapper.class)
  List<LinkHistory> findHistoryListByGroupId(@Bind("groupId") Long groupId);

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
    "insert into link (url, url_hash, sku, name, brand, seller, shipment, status, http_status, platform_id, group_id, account_id) " +
    "values (:link.url, :link.urlHash, :link.sku, :link.name, :link.brand, :link.seller, :link.shipment, :link.status, :link.httpStatus, " +
      ":link.platformId, :link.groupId, :link.accountId)"
  )
  @GetGeneratedKeys
  long insert(@BindBean("link") Link sample);

  @SqlUpdate(
    "insert into link_history (link_id, status, http_status, group_id, account_id) " +
    "values (:link.id, :link.status, :link.httpStatus, :link.groupId, :link.accountId)"
  )
  @GetGeneratedKeys
  long insertHistory(@BindBean("link") Link link);

  @SqlUpdate("update link set pre_status=status, status=:status, status_group=:statusGroup, updated_at=now() where id=:id")
  boolean toggleStatus(@Bind("id") Long id, @Bind("status") LinkStatus status, @Bind("statusGroup") LinkStatusGroup statusGroup);
  
  @SqlQuery("select group_id from link where id in (<linkIdSet>) and account_id=:accountId order by status_group")
  Set<Long> findGroupIdList(@BindList("linkIdSet") Set<Long> linkIdSet, @Bind("accountId") Long accountId);

  @SqlQuery("select group_id from link where id in (<linkIdSet>)")
  Set<Long> findGroupIdSet(@BindList("linkIdSet") Set<Long> linkIdSet);

}
