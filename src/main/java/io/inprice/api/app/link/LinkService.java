package io.inprice.api.app.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.group.GroupAlarmService;
import io.inprice.api.app.group.GroupDao;
import io.inprice.api.app.link.dto.SearchBy;
import io.inprice.api.app.link.dto.SearchDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.GroupDTO;
import io.inprice.api.dto.LinkDeleteDTO;
import io.inprice.api.dto.LinkMoveDTO;
import io.inprice.api.info.Response;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
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

  Response search(SearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, true);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder where = new StringBuilder();

    where.append("where l.account_id = ");
    where.append(dto.getAccountId());

    if (dto.getAlarmStatus() != null && !AlarmStatus.ALL.equals(dto.getAlarmStatus())) {
  		where.append(" and l.alarm_id is ");
    	if (AlarmStatus.ALARMED.equals(dto.getAlarmStatus())) {
    		where.append(" not ");
    	}
    	where.append(" null");
    }
    
    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and ");
    	if (SearchBy.NAME.equals(dto.getSearchBy())) {
    		where.append("IFNULL(l.name, l.url)");
    	} else {
    		where.append(dto.getSearchBy().getFieldName());
    	}
      where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

    if (dto.getLevels() != null && dto.getLevels().size() > 0) {
    	where.append(
  			String.format(" and l.level in (%s) ", io.inprice.common.utils.StringUtils.join("'", dto.getLevels()))
			);
    }

    if (dto.getStatuses() != null && dto.getStatuses().size() > 0) {
    	where.append(
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
          where +
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
  	
  	if (CollectionUtils.isNotEmpty(dto.getLinkIdSet())) dto.getLinkIdSet().remove(null);

    if (CollectionUtils.isNotEmpty(dto.getLinkIdSet())) {
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
   * Moves links from one group to another.
   * 
   * Two operations are done accordingly;
   * 	a) setting new group id for all the selected links 
   * 	b) refreshing group totals
   * 
   */
  Response moveTo(LinkMoveDTO dto) {
  	Response res = Responses.OK;
  	
  	boolean isNewGroup = (dto.getToGroupId() == null && StringUtils.isNotBlank(dto.getToGroupName()));

  	if (isNewGroup || (dto.getToGroupId() != null && dto.getToGroupId() > 0)) {

  		if (CollectionUtils.isNotEmpty(dto.getLinkIdSet())) dto.getLinkIdSet().remove(null);

      if (CollectionUtils.isNotEmpty(dto.getLinkIdSet())) {

      	try (Handle handle = Database.getHandle()) {
        	handle.begin();

      		if (isNewGroup) {
      			dto.setToGroupName(SqlHelper.clear(dto.getToGroupName()));

      			GroupDao groupDao = handle.attach(GroupDao.class);
      			LinkGroup found = groupDao.findByName(dto.getToGroupName(), CurrentUser.getAccountId());
      			if (found == null) { //creating a new group
      				dto.setToGroupId(
    						groupDao.insert(
  								GroupDTO.builder()
  									.name(dto.getToGroupName())
  									.accountId(CurrentUser.getAccountId())
  									.build()
									)
    						);
      			} else {
        			res = Responses.Already.Defined.GROUP;
      			}
      		}

      		if (res.isOK()) {
        		LinkDao linkDao = handle.attach(LinkDao.class);
            
          	Set<Long> foundGroupIdSet = linkDao.findGroupIdSet(dto.getLinkIdSet());

          	if (CollectionUtils.isNotEmpty(foundGroupIdSet)) foundGroupIdSet.remove(null);
          	
          	if (CollectionUtils.isNotEmpty(foundGroupIdSet)) {
        			String joinedIds = StringUtils.join(dto.getLinkIdSet(), ",");
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

            		if (dto.getFromGroupId() != null) { //meaning that it is called from group definition (not from links searching page)
                  GroupDao groupDao = handle.attach(GroupDao.class);
                	LinkGroup group = groupDao.findById(dto.getFromGroupId(), CurrentUser.getAccountId());

                	Map<String, Object> data = new HashMap<>(2);
                	data.put("group", group);
                  data.put("links", linkDao.findListByGroupId(dto.getFromGroupId(), CurrentUser.getAccountId()));
                  res = new Response(data);
            		} else {
            			res = Responses.OK;
            		}
              } else {
              	res = Responses.NotFound.LINK;
    					}
            } else {
            	res = Responses.NotFound.GROUP;
            }
      		}

          if (res.isOK())
          	handle.commit();
          else
          	handle.rollback();
      	}
      } else {
      	res = Responses.NotFound.LINK;
      }
    } else {
    	res = Responses.NotFound.GROUP;
    }

  	return res;
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

}
