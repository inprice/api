package io.inprice.api.app.superuser.link;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.superuser.link.dto.BulkChangetDTO;
import io.inprice.api.app.superuser.link.dto.SearchDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.Pair;
import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.models.LinkPrice;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.utils.StringHelper;
import io.inprice.common.repository.PlatformDao;

class LinkService {

  private static final Logger logger = LoggerFactory.getLogger(LinkService.class);

  private final Set<LinkStatus> STATUSES_FOR_CHANGE;
  
  public LinkService() {
  	STATUSES_FOR_CHANGE = Set.of(
			LinkStatus.REFRESHED,
			LinkStatus.RESOLVED,
			LinkStatus.PAUSED,
			LinkStatus.NOT_SUITABLE
		);
	}

  Response search(SearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, false);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder where = new StringBuilder("where 1=1 ");
    
    if (dto.getWorkspaceId() != null) {
    	where.append(" and l.workspace_id = ");
    	where.append(dto.getWorkspaceId());
    }

    if (dto.getAlarmStatus() != null && !AlarmStatus.ALL.equals(dto.getAlarmStatus())) {
  		where.append(" and l.alarm_id is ");
    	if (AlarmStatus.ALARMED.equals(dto.getAlarmStatus())) {
    		where.append(" not ");
    	}
    	where.append(" null");
    }

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and CONCAT(ifnull(l.name, ''), ifnull(l.sku, ''), ifnull(l.seller, ''), ifnull(l.brand, ''))");
      where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

    if (CollectionUtils.isNotEmpty(dto.getStatuses())) {
    	where.append(
		    String.format(" and l.status in (%s) ", StringHelper.join("'", dto.getStatuses()))
			);
    }

    //---------------------------------------------------
    //fetching the data
    //---------------------------------------------------
    try (Handle handle = Database.getHandle()) {
      List<Link> searchResult =
        handle.createQuery(
          "select l.*" + PlatformDao.FIELDS + ", p.name as product_name from link as l " + 
      		"inner join product as p on p.id = l.product_id " + 
      		"left join platform as pl on pl.id = l.platform_id " + 
          where +
          " order by " + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir() + ", l.id " +
          " limit " + dto.getRowCount() + ", " + dto.getRowLimit()
        )
      .map(new LinkMapper())
      .list();
      
      return new Response(searchResult);
    } catch (Exception e) {
      logger.error("Failed in full search for links.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  Response fetchDetails(Long id) {
    Response res = Responses.NotFound.LINK;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        LinkDao linkDao = handle.attach(LinkDao.class);

        Link link = linkDao.findById(id);
        if (link != null) {
          List<LinkSpec> specList = linkDao.findSpecListByLinkId(link.getId());
          List<LinkPrice> priceList = linkDao.findPriceListByLinkId(link.getId());
          List<LinkHistory> historyList = linkDao.findHistoryListByLinkId(link.getId());

          if (specList == null) specList = new ArrayList<>();
          if (priceList == null) priceList = new ArrayList<>();
          if (historyList == null) historyList = new ArrayList<>();
        
          if (StringUtils.isNotBlank(link.getSku())) specList.add(0, new LinkSpec("Sku", link.getSku()));
          if (StringUtils.isNotBlank(link.getBrand())) specList.add(0, new LinkSpec("Brand", link.getBrand()));
          
          Map<String, Object> data = Map.of(
          	"specList", specList,
          	"priceList", priceList,
          	"historyList", historyList
        	);
          res = new Response(data);
        }
      }
    }

    return res;
  }

  Response changeStatus(BulkChangetDTO dto) {
    Response res = Responses.NotSuitable.LINK;

    if (CollectionUtils.isNotEmpty(dto.getIdSet())) {
    	if (dto.getStatus() != null && STATUSES_FOR_CHANGE.contains(dto.getStatus())) {
        try (Handle handle = Database.getHandle()) {
  
          LinkDao linkDao = handle.attach(LinkDao.class);
          List<Pair<Long, String>> idAndStatusList = linkDao.findIdAndStatusesByIdSet(dto.getIdSet());
  
          Set<Long> selectedSet = new HashSet<>();
          
          for (Pair<Long, String> pair: idAndStatusList) {
          	if (LinkStatus.RESOLVED.equals(dto.getStatus())) {
          		if (LinkStatus.TOBE_IMPLEMENTED.name().equals(pair.getRight())) {
            		selectedSet.add(pair.getLeft());
            	}
          	} else if (dto.getStatus().name().equals(pair.getRight()) == false) {
          		selectedSet.add(pair.getLeft());
          	}
          }
  
          if (CollectionUtils.isNotEmpty(selectedSet)) {
          	handle.begin();

          	int affected = linkDao.setStatus(selectedSet, dto.getStatus(), dto.getStatus().getGrup());
  
          	if (affected == selectedSet.size()) {
            	linkDao.insertHistory(selectedSet);
            	handle.commit();
            	res = Responses.OK;
            } else {
            	handle.rollback();
            	res = Responses.DataProblem.DB_PROBLEM;
            }
          }

        }
      } else {
      	res = new Response("Acceptable statuses: REFRESHED, PAUSED, RESOLVED and NOT_SUITABLE!");
      }
    }

    return res;
  }

  Response undo(BulkChangetDTO dto) {
    Response res = Responses.NotFound.LINK;

    if (CollectionUtils.isNotEmpty(dto.getIdSet())) {
      try (Handle handle = Database.getHandle()) {

      	handle.begin();
      	int affected = 0;

        LinkDao linkDao = handle.attach(LinkDao.class);
        for (Long id: dto.getIdSet()) {
        	List<LinkHistory> historyList = linkDao.findHistoryListByLinkId(id);

          if (CollectionUtils.isNotEmpty(historyList)) {
          	LinkHistory lastHistory = historyList.get(0);

          	if (historyList.size() > 1 && STATUSES_FOR_CHANGE.contains(lastHistory.getStatus())) {
            	LinkHistory preHistory = historyList.get(1);
            	affected += linkDao.setStatus(id, preHistory.getStatus(), preHistory.getStatus().getGrup());
            	linkDao.deleteHistory(lastHistory.getId());
            }
          }
        }

        if (affected > 0) {
        	handle.commit();
        	res = Responses.OK;
        } else {
        	handle.rollback();
        	res = new Response("No suitable link found for undo!");
        }
      }
    }

    return res;
  }

}
