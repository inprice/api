package io.inprice.api.app.link;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.link.dto.LinkSearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LinkDTO;
import io.inprice.api.dto.LinkDeleteDTO;
import io.inprice.api.dto.LinkMoveDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.repository.CommonDao;
import io.inprice.common.utils.DateUtils;

class LinkService {

  private static final Logger log = LoggerFactory.getLogger(LinkService.class);

  private final UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });

  Response insert(LinkDTO dto) {
    if (dto != null) {
      Response res = validate(dto);

      if (res.isOK()) {
        try (Handle handle = Database.getHandle()) {
          LinkDao linkDao = handle.attach(LinkDao.class);

          String urlHash = DigestUtils.md5Hex(dto.getUrl());
          Link link = linkDao.findByGroupIdAndUrlHash(dto.getGroupId(), urlHash);
          if (link == null) {

            Link sample = linkDao.findSampleByUrlHashAndStatus(urlHash, LinkStatus.AVAILABLE);
            if (sample != null) { // if any, lets clone it
              long id = linkDao.insert(link, dto.getGroupId(), CurrentUser.getAccountId());
              if (id > 0) {
                sample.setId(id);
                linkDao.insertHistory(sample);
                res = new Response(sample);
              }
            } else {
              long id = linkDao.insert(dto.getUrl(), urlHash, dto.getGroupId(), CurrentUser.getAccountId());
              if (id > 0) {
                sample = new Link();
                sample.setId(id);
                sample.setUrl(dto.getUrl());
                sample.setUrlHash(urlHash);
                sample.setGroupId(dto.getGroupId());
                sample.setAccountId(CurrentUser.getAccountId());
                linkDao.insertHistory(sample);
                res = new Response(sample);
              }
            }

            if (sample.getId() != null) { //meaning that it is inserted successfully!
            	int linkCount = handle.attach(AccountDao.class).findLinkCount(CurrentUser.getAccountId());
              Map<String, Object> data = new HashMap<>(2);
              data.put("model", sample);
              data.put("linkCount", linkCount);
              res = new Response(data);
            }

          } else {
            res = Responses.DataProblem.ALREADY_EXISTS;
          }
        }
      }
      return res;
    }
    return Responses.Invalid.LINK;
  }

  public Response fullSearch(LinkSearchDTO dto) {
    clearSearchDto(dto);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder criteria = new StringBuilder();

    criteria.append("where l.account_id = ");
    criteria.append(CurrentUser.getAccountId());

    if (StringUtils.isNotBlank(dto.getTerm())) {
      criteria.append(" and l.sku like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or l.name like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or l.brand like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or l.seller like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or plt.domain like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' ");
    }

    if (dto.getStatuses() != null && dto.getStatuses().length > 0) {
      criteria.append(
        String.format(" and l.status_group in ('%s') ", String.join("', '", dto.getStatuses()))
      );
    }

    //limiting
    String limit = " limit " + Consts.ROW_LIMIT_FOR_LISTS;
    if (dto.getLoadMore() && dto.getRowCount() >= Consts.ROW_LIMIT_FOR_LISTS) {
      limit = " limit " + dto.getRowCount() + ", " + Consts.ROW_LIMIT_FOR_LISTS;
    }

    //---------------------------------------------------
    //fetching the data
    //---------------------------------------------------
    try (Handle handle = Database.getHandle()) {
      List<Link> searchResult =
        handle.createQuery(
          "select *, plt.domain as platform from link as l " + 
      		"left join platform as plt on plt.id = l.platform_id " + 
          criteria +
          " order by l.status_group, l.updated_at, plt.domain " +
          limit
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
  	Response[] res = { Responses.NotFound.LINK };

    if (dto != null && dto.getLinkIdSet() != null && dto.getLinkIdSet().size() > 0) {
    	int count = dto.getLinkIdSet().size();
    	
    	String joinedIds = StringUtils.join(dto.getLinkIdSet(), ",");
    	
      final String where = String.format("where link_id in (%s) and account_id=%d ", joinedIds, CurrentUser.getAccountId());

      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transaction -> {
          LinkDao linkDao = transaction.attach(LinkDao.class);

          Batch batch = transaction.createBatch();
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
          
          if (result[3] > 0) {
          	CommonDao commonDao = transaction.attach(CommonDao.class);
          	if (dto.getFromGroupId() != null) { //single group
          		commonDao.refreshGroup(dto.getFromGroupId());
          	} else {
              Set<Long> groupIdSet = linkDao.findGroupIdSet(dto.getLinkIdSet());
            	if (groupIdSet != null && groupIdSet.size() > 0) {
            		for (Long groupId: groupIdSet) {
            			commonDao.refreshGroup(groupId);
          			}
              }
          	}
        		res[0] = getResponseWithLinkCount(transaction);
          }

          return res[0].isOK();
        });
      }

    }
  	return res[0];
  }

  /**
   * Moves links from one to another.
   * Two operation is enough; a) setting new group id for all the selected links and b) refreshing group numbers
   * 
   */
  Response moveTo(LinkMoveDTO dto) {
  	Response[] res = { Responses.Invalid.DATA };

  	if (dto != null && dto.getToGroupId() != null && dto.getToGroupId() > 0) {
      if (dto.getLinkIdSet() != null && dto.getLinkIdSet().size() > 0) {

      	try (Handle handle = Database.getHandle()) {
        	handle.inTransaction(transaction -> {
            LinkDao linkDao = transaction.attach(LinkDao.class);
  
          	Set<Long> foundGroupIdSet = linkDao.findGroupIdList(dto.getLinkIdSet(), CurrentUser.getAccountId());
          	if (foundGroupIdSet != null && foundGroupIdSet.size() > 0) {
          		int affected = linkDao.changeGroupId(dto.getLinkIdSet(), dto.getToGroupId());
          		if (affected == dto.getLinkIdSet().size()) {
          			CommonDao commonDao = transaction.attach(CommonDao.class);
          			
            		foundGroupIdSet.add(dto.getToGroupId());
            		for (Long groupId: foundGroupIdSet) {
            			commonDao.refreshGroup(groupId);
            		}

            		if (dto.getFromGroupId() != null) {
                	Map<String, Object> dataMap = new HashMap<>(1);
                  dataMap.put("links", LinkHelper.findDetailedLinkList(dto.getFromGroupId(), handle.attach(LinkDao.class)));
                  res[0] = new Response(dataMap);
            		} else { // meaning that it is called from link search page (not from detailed group page)!
            			res[0] = Responses.OK;
            		}
          		}
            }
            return true;
        	});
      	}
      }
    }

  	return res[0];
  }

  private Response getResponseWithLinkCount(Handle trans) {
  	AccountDao accountDao = trans.attach(AccountDao.class);
  	int linkCount = accountDao.findLinkCount(CurrentUser.getAccountId());
    Map<String, Object> data = new HashMap<>(1);
    data.put("linkCount", linkCount);
    return new Response(data);
  }

  Response toggleStatus(Long id) {
    Response[] res = { Responses.NotFound.LINK };

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transaction -> {
          LinkDao linkDao = transaction.attach(LinkDao.class);

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
                  res[0] = Responses.DataProblem.TOO_MANY_TOGGLING;
                }
              }
            }

            if (! res[0].equals(Responses.DataProblem.TOO_MANY_TOGGLING)) {
              LinkStatus newStatus = (LinkStatus.PAUSED.equals(link.getStatus()) ? link.getPreStatus() : LinkStatus.PAUSED);
              boolean isOK = linkDao.toggleStatus(id, newStatus, newStatus.getGroup());
              if (isOK) {
                link.setPreStatus(link.getStatus());
                link.setStatus(newStatus);
                long historyId = linkDao.insertHistory(link);
                if (historyId > 0) {
            			CommonDao commonDao = transaction.attach(CommonDao.class);
            			commonDao.refreshGroup(link.getGroupId());
            			res[0] = Responses.OK;
                }
              }
            }

          }
          return res[0].isOK();
        });
      }
    }

    return res[0];
  }

  Response getDetails(Long id) {
    Response res = Responses.NotFound.LINK;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        LinkDao linkDao = handle.attach(LinkDao.class);

        Link link = linkDao.findById(id);
        if (link != null) {

          Map<String, Object> data = new HashMap<>(4);
          List<LinkHistory> historyList = linkDao.findHistoryListByLinkId(id);
          if (historyList != null && historyList.size() > 0) {
            data.put("link", link);
            data.put("historyList", historyList);
            data.put("priceList", linkDao.findPriceListByLinkId(id));
            data.put("specList", linkDao.findSpecListByLinkId(id));
            res = new Response(data);
          }
        }
      }
    }

    return res;
  }

  private Response validate(LinkDTO dto) {
    String problem = null;

    if (!urlValidator.isValid(dto.getUrl())) {
      problem = "Invalid URL!";
    }

    if (problem == null) {
      if (dto.getGroupId() == null || dto.getGroupId() < 1) {
        problem = "Group id cannot be null!";
      }
    }

    if (problem == null) {
      dto.setUrl(SqlHelper.clear(dto.getUrl()));
      return Responses.OK;
    } else {
      return new Response(problem);
    }
  }

  private void clearSearchDto(LinkSearchDTO dto) {
    dto.setTerm(SqlHelper.clear(dto.getTerm()));
    if (dto.getStatuses() != null && dto.getStatuses().length > 0) {
      Set<String> newStatusSet = new HashSet<>(dto.getStatuses().length);
      Set<String> linkNamesSet = EnumUtils.getEnumMap(LinkStatus.class).keySet();
	
      for (int i = 0; i < dto.getStatuses().length; i++) {
        String status = dto.getStatuses()[i];
        if (StringUtils.isNotBlank(status)) {
          if (linkNamesSet.contains(status)) newStatusSet.add(status);
        }
      }
      dto.setStatuses(newStatusSet.toArray(new String[0]));
    }
  }

}
