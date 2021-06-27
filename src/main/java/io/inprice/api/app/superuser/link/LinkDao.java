package io.inprice.api.app.superuser.link;

import java.util.List;
import java.util.Set;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;

import io.inprice.common.info.Pair;
import io.inprice.common.mappers.IdNamePairMapper;
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

public interface LinkDao {
	
  @SqlQuery("select * from link where id=:id")
  @UseRowMapper(LinkMapper.class)
  Link findById(@Bind("id") Long id);

	@SqlQuery("select id, status as name from link where id in (<idSet>)")
  @UseRowMapper(IdNamePairMapper.class)
  List<Pair<Long, String>> findIdAndStatusesByIdSet(@BindList("idSet") Set<Long> idSet);

	@SqlQuery("select * from link where id in (<idSet>)")
  @UseRowMapper(LinkMapper.class)
  List<Link> findListByIdSet(@BindList("idSet") Set<Long> idSet);

  @SqlUpdate("update link set pre_status=status, status=:status, status_group=:statusGroup, updated_at=now() where id in (<idSet>)")
  int setStatus(@BindList("idSet") Set<Long> idSet, @Bind("status") LinkStatus status, @Bind("statusGroup") LinkStatusGroup statusGroup);

  @SqlUpdate("update link set pre_status=status, status=:status, status_group=:statusGroup, updated_at=now() where id=:id")
  int setStatus(@Bind("id") Long id, @Bind("status") LinkStatus status, @Bind("statusGroup") LinkStatusGroup statusGroup);

  @SqlUpdate(
		"insert into link_history (link_id, status, http_status, group_id, account_id) " +
		"select id, status, http_status, group_id, account_id from link where id in (<idSet>) "
	)
  int insertHistory(@BindList("idSet") Set<Long> idSet);

  @SqlUpdate("delete from link_history where id=:historyId")
  boolean deleteHistory(@Bind("historyId") Long historyId);

  @SqlQuery("select * from link_history where link_id=:linkId order by id desc")
  @UseRowMapper(LinkHistoryMapper.class)
  List<LinkHistory> findHistoryListByLinkId(@Bind("linkId") Long linkId);

  @SqlQuery("select * from link_price where link_id=:linkId order by id desc")
  @UseRowMapper(LinkPriceMapper.class)
  List<LinkPrice> findPriceListByLinkId(@Bind("linkId") Long linkId);

  @SqlQuery("select * from link_spec where link_id=:linkId order by _key")
  @UseRowMapper(LinkSpecMapper.class)
  List<LinkSpec> findSpecListByLinkId(@Bind("linkId") Long linkId);
  
}
