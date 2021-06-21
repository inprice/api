package io.inprice.api.app.link;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.group.GroupAlarmService;
import io.inprice.api.app.group.GroupDao;
import io.inprice.api.app.link.dto.SearchDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LinkDeleteDTO;
import io.inprice.api.dto.LinkMoveDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkGroup;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.models.LinkPrice;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.repository.AlarmDao;
import io.inprice.common.repository.PlatformDao;

class LinkService {

  private static final Logger log = LoggerFactory.getLogger(LinkService.class);

  public Response search(SearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, true);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder crit = new StringBuilder();

    crit.append("where l.account_id = ");
    crit.append(dto.getAccountId());

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	crit.append(" and ");
    	crit.append(dto.getSearchBy().getFieldName());
      crit.append(" like '%");
      crit.append(dto.getTerm());
      crit.append("%' ");
    }

    if (dto.getLevels() != null && dto.getLevels().size() > 0) {
    	crit.append(
  			String.format(" and l.level in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getLevels()))
			);
    }

    if (dto.getStatuses() != null && dto.getStatuses().size() > 0) {
    	crit.append(
		    String.format(" and status_group in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getStatuses()))
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

  Response delete(LinkDeleteDTO dto) {
  	Response response = Responses.NotFound.LINK;

    if (dto != null && dto.getLinkIdSet() != null && dto.getLinkIdSet().size() > 0) {
    	int count = dto.getLinkIdSet().size();
    	
    	String joinedIds = StringUtils.join(dto.getLinkIdSet(), ",");
    	
      final String where = String.format("where link_id in (%s) and account_id=%d ", joinedIds, CurrentUser.getAccountId());

      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        Batch batch = handle.createBatch();
        batch.add("delete from alarm " + where);
        batch.add("delete from link_price " + where);
        batch.add("delete from link_history " + where);
        batch.add("delete from link_spec " + where);
        batch.add("delete from link " + where.replace("link_", "")); //important!!!
				batch.add(
					String.format(
						"update account set link_count=link_count-%d where id=%d",
						count, CurrentUser.getAccountId()
					)
				);
				int[] result = batch.execute();

        if (result[4] > 0) {
        	LinkDao linkDao = handle.attach(LinkDao.class);

        	//refreshes groups' totals and alarm if needed!
        	Set<Long> groupIdSet = linkDao.findGroupIdSet(dto.getLinkIdSet());
        	if (CollectionUtils.isNotEmpty(groupIdSet)) {
        		GroupAlarmService.updateAlarm(groupIdSet, handle);
        	}

          if (dto.getFromGroupId() != null) { //meaning that it is called from group definition (not from links search page)
          	LinkGroup group = handle.attach(GroupDao.class).findByIdWithAlarm(dto.getFromGroupId(), CurrentUser.getAccountId());
          	Map<String, Object> data = new HashMap<>(1);
            data.put("group", group);
            response = new Response(data);
          } else { //from links page
          	response = Responses.OK;
          }
        }

        if (response.isOK())
        	handle.commit();
        else
        	handle.rollback();
      }

    }
  	return response;
  }

  /**
   * Moves links from one to another.
   * Two operation is enough; a) setting new group id for all the selected links and b) refreshing group numbers
   * 
   */
  Response moveTo(LinkMoveDTO dto) {
  	Response response = Responses.Invalid.DATA;

  	if (dto != null && (dto.getToGroupId() != null && dto.getToGroupId() > 0) || StringUtils.isNotBlank(dto.getToGroupName())) {
      if (dto.getLinkIdSet() != null && dto.getLinkIdSet().size() > 0) {

      	try (Handle handle = Database.getHandle()) {
        	handle.begin();

      		if (dto.getToGroupId() == null && StringUtils.isNotBlank(dto.getToGroupName())) {
      			GroupDao groupDao = handle.attach(GroupDao.class);
      			LinkGroup found = groupDao.findByName(dto.getToGroupName().trim(), CurrentUser.getAccountId());
      			if (found == null) { //creating a new group
      				dto.setToGroupId(groupDao.insert(dto.getToGroupName().trim(), BigDecimal.ZERO, CurrentUser.getAccountId()));
      			} else {
        			response = Responses.Already.Defined.GROUP;
      			}
      		}

      		if (!response.equals(Responses.Already.Defined.GROUP)) {
        		LinkDao linkDao = handle.attach(LinkDao.class);
            
          	Set<Long> foundGroupIdSet = linkDao.findGroupIdSet(dto.getLinkIdSet());
          	if (CollectionUtils.isNotEmpty(foundGroupIdSet)) {
        			String joinedIds = dto.getLinkIdSet().stream().map(String::valueOf).collect(Collectors.joining(","));
        			final String 
        				updatePart = 
        					String.format(
      							"set group_id=%d where link_id in (%s) and group_id!=%d and account_id=%d", 
      							dto.getToGroupId(), joinedIds, dto.getToGroupId(), CurrentUser.getAccountId()
    							);
        			
              Batch batch = handle.createBatch();
              batch.add("update alarm " + updatePart);
              batch.add("update link_price " + updatePart);
              batch.add("update link_history " + updatePart);
              batch.add("update link_spec " + updatePart);
              batch.add("update link " + updatePart.replace("link_", "")); //important!!!
    					int[] result = batch.execute();

    					if (result[4] > 0) {
            		foundGroupIdSet.add(dto.getToGroupId());

            		//refreshes groups' totals and alarm if needed!
              	Set<Long> groupIdSet = linkDao.findGroupIdSet(dto.getLinkIdSet());
              	if (CollectionUtils.isNotEmpty(groupIdSet)) {
              		GroupAlarmService.updateAlarm(groupIdSet, handle);
              	}

            		if (dto.getFromGroupId() != null) { //meaning that it is called from group definition (not from links search page)
                  GroupDao groupDao = handle.attach(GroupDao.class);
                	LinkGroup group = groupDao.findById(dto.getFromGroupId(), CurrentUser.getAccountId());

                	Map<String, Object> data = new HashMap<>(2);
                	data.put("group", group);
                  data.put("links", linkDao.findListByGroupId(dto.getFromGroupId(), CurrentUser.getAccountId()));
                  response = new Response(data);
            		} else {
            			response = Responses.OK;
            		}
    					}
            }
      		}

          if (response.isOK())
          	handle.commit();
          else
          	handle.rollback();
      	}
      }
    }

  	return response;
  }

  Response getDetails(Long id) {
    Response res = Responses.NotFound.LINK;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        LinkDao linkDao = handle.attach(LinkDao.class);

        Link link = linkDao.findById(id, CurrentUser.getAccountId());
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

  /*
  Response toggleStatus(Long id) {
    Response response = Responses.NotFound.LINK;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        LinkDao linkDao = handle.attach(LinkDao.class);
        Link link = linkDao.findById(id);

        if (link != null) {

          //check if he tries too much
          List<LinkHistory> lastThreeList = linkDao.findLastThreeHistoryRowsByLinkId(id);
          if (lastThreeList.size() == 3) {
            LinkHistory row0 = lastThreeList.get(0);
            LinkHistory row2 = lastThreeList.get(2);
            if (row0.getStatus().equals(row2.getStatus())) {
              Date now = new Date();
              long diff0 = DateUtils.findDayDiff(row0.getCreatedAt(), now);
              long diff2 = DateUtils.findDayDiff(row2.getCreatedAt(), now);
              if (diff0 == 0 && diff2 == 0) {
                response = Responses.DataProblem.TOO_MANY_TOGGLING;
              }
            }
          }

          if (! response.equals(Responses.DataProblem.TOO_MANY_TOGGLING)) {
            LinkStatus newStatus = (LinkStatus.PAUSED.equals(link.getStatus()) ? link.getPreStatus() : LinkStatus.PAUSED);
            boolean isOK = linkDao.toggleStatus(id, newStatus, newStatus.getGroup());
            if (isOK) {
              link.setPreStatus(link.getStatus());
              link.setStatus(newStatus);
              long historyId = linkDao.insertHistory(link);
              if (historyId > 0) {
          			CommonDao commonDao = handle.attach(CommonDao.class);
          			commonDao.refreshGroup(link.getGroupId());
          			response = Responses.OK;
              }
            }
          }

        }

        if (response.isOK())
        	handle.commit();
        else
        	handle.rollback();
      }
    }

    return response;
  }
*/

}
