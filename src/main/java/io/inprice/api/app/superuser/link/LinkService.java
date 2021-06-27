package io.inprice.api.app.superuser.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import io.inprice.api.app.superuser.link.dto.SearchBy;
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
import io.inprice.common.repository.AlarmDao;
import io.inprice.common.repository.PlatformDao;

class LinkService {

  private static final Logger log = LoggerFactory.getLogger(LinkService.class);

  private final Set<LinkStatus> STATUSES_FOR_CHANGE;
  
  public LinkService() {
  	STATUSES_FOR_CHANGE = new HashSet<>(3);
  	STATUSES_FOR_CHANGE.add(LinkStatus.RESOLVED);
  	STATUSES_FOR_CHANGE.add(LinkStatus.PAUSED);
  	STATUSES_FOR_CHANGE.add(LinkStatus.NOT_SUITABLE);
	}

  Response search(SearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, false);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder crit = new StringBuilder("where 1=1 ");
    
    if (dto.getAccountId() != null) {
    	crit.append(" and l.account_id = ");
    	crit.append(dto.getAccountId());
    }

    if (dto.getAlarmStatus() != null && !AlarmStatus.ALL.equals(dto.getAlarmStatus())) {
  		crit.append(" and l.alarm_id is ");
    	if (AlarmStatus.ALARMED.equals(dto.getAlarmStatus())) {
    		crit.append(" not ");
    	}
    	crit.append(" null");
    }

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	crit.append(" and ");
    	if (SearchBy.NAME.equals(dto.getSearchBy())) {
    		crit.append("IFNULL(l.name, l.url)");
    	} else {
    		crit.append(dto.getSearchBy().getFieldName());
    	}
      crit.append(" like '%");
      crit.append(dto.getTerm());
      crit.append("%' ");
    }

    if (dto.getStatuses() != null && dto.getStatuses().size() > 0) {
    	crit.append(
		    String.format(" and status in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getStatuses()))
			);
    }

    //---------------------------------------------------
    //fetching the data
    //---------------------------------------------------
    try (Handle handle = Database.getHandle()) {
      List<Link> searchResult =
        handle.createQuery(
          "select l.*" + PlatformDao.FIELDS + AlarmDao.FIELDS + ", g.name as group_name from link as l " + 
      		"inner join link_group as g on g.id = l.group_id " + 
      		"left join platform as p on p.id = l.platform_id " + 
          "left join alarm as al on al.id = l.alarm_id " + 
          crit +
          " order by " + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir() +
          " limit " + dto.getRowCount() + ", " + dto.getRowLimit()
        )
      .map(new LinkMapper())
      .list();

      return new Response(Collections.singletonMap("rows", searchResult));
    } catch (Exception e) {
      log.error("Failed in full search for links.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  Response getDetails(Long id) {
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
        
          if (StringUtils.isNotBlank(link.getSku())) specList.add(0, new LinkSpec("Code", link.getSku()));
          if (StringUtils.isNotBlank(link.getBrand())) specList.add(0, new LinkSpec("Brand", link.getBrand()));
          
          Map<String, Object> data = new HashMap<>(3);
          data.put("specList", specList);
          data.put("priceList", priceList);
          data.put("historyList", historyList);
          res = new Response(data);
        }
      }
    }

    return res;
  }

  Response changeStatus(BulkChangetDTO dto) {
    Response response = Responses.NotFound.LINK;

  	if (dto.getStatus() != null && STATUSES_FOR_CHANGE.contains(dto.getStatus())) {
      if (CollectionUtils.isNotEmpty(dto.getIdSet())) {
        try (Handle handle = Database.getHandle()) {
  
          LinkDao linkDao = handle.attach(LinkDao.class);
          List<Pair<Long, String>> idAndStatusList = linkDao.findIdAndStatusesByIdSet(dto.getIdSet());
  
          Set<Long> selectedSet = new HashSet<>();
          
          for (Pair<Long, String> pair: idAndStatusList) {
          	if (! LinkStatus.RESOLVED.equals(dto.getStatus()) || LinkStatus.TOBE_IMPLEMENTED.name().equals(pair.getRight())) {
        			selectedSet.add(pair.getLeft());
          	}
          }
  
          if (selectedSet.size() > 0) {
          	handle.begin();
          	int affected = 0;
          	
          	if (CollectionUtils.isNotEmpty(selectedSet)) {
          		affected = linkDao.setStatus(selectedSet, dto.getStatus(), dto.getStatus().getGroup());
          	}
  
            if (affected == selectedSet.size()) {
            	linkDao.insertHistory(selectedSet);
            	handle.commit();
            	response = Responses.OK;
            } else {
            	handle.rollback();
            	response = Responses.DataProblem.DB_PROBLEM;
            }
          } else {
          	response = new Response("No suitable link found!");
          }
        }
      }
    } else {
    	response = new Response("New status is not proper!");
    }

    return response;
  }

  Response undo(BulkChangetDTO dto) {
    Response response = Responses.NotFound.LINK;

    if (CollectionUtils.isNotEmpty(dto.getIdSet())) {
      try (Handle handle = Database.getHandle()) {

      	handle.begin();
      	int affected = 0;

        LinkDao linkDao = handle.attach(LinkDao.class);
        for (Long id: dto.getIdSet()) {
        	List<LinkHistory> historyList = linkDao.findHistoryListByLinkId(id);

          if (historyList != null) {
          	LinkHistory lastHistory = historyList.get(0);
          	if (historyList.size() > 1 && STATUSES_FOR_CHANGE.contains(lastHistory.getStatus())) {

            	LinkHistory preHistory = historyList.get(1);
            	affected += linkDao.setStatus(id, preHistory.getStatus(), preHistory.getStatus().getGroup());
            	linkDao.deleteHistory(lastHistory.getId());
            }
          }
        }

        if (affected > 0) {
        	handle.commit();
        	response = Responses.OK;
        } else {
        	handle.rollback();
        	response = new Response("Link(s) status isn't suitable for undo!");
        }
      }
    }

    return response;
  }

}
