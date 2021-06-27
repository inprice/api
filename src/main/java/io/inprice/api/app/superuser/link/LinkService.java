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

import io.inprice.api.app.superuser.link.dto.SearchBy;
import io.inprice.api.app.superuser.link.dto.SearchDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.IdSetDTO;
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

  public Response search(SearchDTO dto) {
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

  Response resolved(IdSetDTO dto) {
    Response response = Responses.NotFound.LINK;

    if (CollectionUtils.isNotEmpty(dto.getSet())) {
      try (Handle handle = Database.getHandle()) {

        LinkDao linkDao = handle.attach(LinkDao.class);
        List<Pair<Long, String>> idAndStatusList = linkDao.findIdAndStatusesByIdSet(dto.getSet());

        Set<Long> selectedSet = new HashSet<>();
        
        for (Pair<Long, String> pair: idAndStatusList) {
        	if (LinkStatus.TOBE_IMPLEMENTED.name().equals(pair.getRight())) {
        		selectedSet.add(pair.getLeft());
        	}
        }

        if (selectedSet.size() > 0) {
        	handle.begin();
        	int affected = 0;
        	
        	if (CollectionUtils.isNotEmpty(selectedSet)) {
        		affected = linkDao.setStatus(selectedSet, LinkStatus.RESOLVED, LinkStatus.RESOLVED.getGroup());
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
        	response = new Response("No suitable link to be RESOLVED found!");
        }
      }
    }

    return response;
  }

  Response toggleStatus(IdSetDTO dto) {
    Response response = Responses.NotFound.LINK;

    if (CollectionUtils.isNotEmpty(dto.getSet())) {
      try (Handle handle = Database.getHandle()) {

        LinkDao linkDao = handle.attach(LinkDao.class);
        List<Link> linkList = linkDao.findListByIdSet(dto.getSet());

        List<Link> pausedList = new ArrayList<>();
        Set<Long> allIdSet = new HashSet<>();
        Set<Long> othersSet = new HashSet<>();
        
        for (Link link: linkList) {
        	allIdSet.add(link.getId());
        	if (LinkStatus.PAUSED.equals(link.getStatus())) {
        		pausedList.add(link);
        	} else {
        		othersSet.add(link.getId());
        	}
        }

      	handle.begin();
      	int affected = 0;
      	
      	if (CollectionUtils.isNotEmpty(pausedList)) {
      		for (Link link: pausedList) {
      			affected += linkDao.setStatus(link.getId(), link.getPreStatus(), link.getPreStatus().getGroup());
      		}
      	}

        if (CollectionUtils.isNotEmpty(othersSet)) {
        	affected += linkDao.setStatus(othersSet, LinkStatus.PAUSED, LinkStatus.PAUSED.getGroup());
        }

        if (affected == allIdSet.size()) {
        	linkDao.insertHistory(allIdSet);
        	handle.commit();
        	response = Responses.OK;
        } else {
        	handle.rollback();
        	response = Responses.DataProblem.DB_PROBLEM;
        }
      }
    }

    return response;
  }

}
