package io.inprice.api.app.group;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.link.LinkDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.GroupDTO;
import io.inprice.api.dto.LinkBulkInsertDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.converters.GroupRefreshResultConverter;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.info.GroupRefreshResult;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkGroup;
import io.inprice.common.repository.CommonDao;
import io.inprice.common.utils.URLUtils;

class GroupService {

  private static final Logger log = LoggerFactory.getLogger(GroupService.class);
	
  Response findById(Long id) {
    try (Handle handle = Database.getHandle()) {
      GroupDao groupDao = handle.attach(GroupDao.class);

      LinkGroup group = groupDao.findByIdWithAlarm(id, CurrentUser.getAccountId());
      if (group != null) {
        return new Response(group);
      }
      return Responses.NotFound.GROUP;
    }
  }
  
  Response getIdNameList(Long exclude) {
  	try (Handle handle = Database.getHandle()) {
  		GroupDao groupDao = handle.attach(GroupDao.class);
  		return new Response(groupDao.getIdNameList((exclude != null ? exclude : 0), CurrentUser.getAccountId()));
  	}
  }

  Response search(BaseSearchDTO dto) {
    try (Handle handle = Database.getHandle()) {
      GroupDao groupDao = handle.attach(GroupDao.class);
      return new Response(groupDao.search(DTOHelper.normalizeSearch(dto, true, true)));
    }
  }

  Response findLinksById(Long id) {
    try (Handle handle = Database.getHandle()) {
    	GroupDao groupDao = handle.attach(GroupDao.class);
      LinkDao linkDao = handle.attach(LinkDao.class);

      LinkGroup group = groupDao.findByIdWithAlarm(id, CurrentUser.getAccountId());
      if (group != null) {
      	Map<String, Object> dataMap = new HashMap<>(2);
        dataMap.put("group", group);
        dataMap.put("links", linkDao.findListByGroupId(id, CurrentUser.getAccountId()));
        return new Response(dataMap);
      }
    }
    return Responses.NotFound.GROUP;
  }

  Response insert(GroupDTO dto) {
    if (dto != null) {
      String problem = validate(dto);
      if (problem == null) {
        try (Handle handle = Database.getHandle()) {
          GroupDao groupDao = handle.attach(GroupDao.class);

          LinkGroup found = groupDao.findByName(dto.getName(), CurrentUser.getAccountId());
          if (found == null) {
          	Long id = 
              groupDao.insert(
                dto.getName(),
                dto.getPrice(),
                dto.getAccountId()
              );
          	if (id != null && id > 0) {
          		found = groupDao.findById(id, CurrentUser.getAccountId());
              Map<String, LinkGroup> data = new HashMap<>(1);
              data.put("group", found);
              return new Response(data);
            }
          } else {
          	return Responses.Already.Defined.GROUP;
          }
        }
        return Responses.DataProblem.DB_PROBLEM;
      } else {
        return new Response(problem);
      }
    }
    return Responses.Invalid.GROUP;
  }

  Response update(GroupDTO dto) {
    if (dto != null && dto.getId() != null && dto.getId() > 0) {

      String problem = validate(dto);
      if (problem == null) {

        Response response = Responses.DataProblem.DB_PROBLEM;

        try (Handle handle = Database.getHandle()) {
        	handle.begin();

          GroupDao groupDao = handle.attach(GroupDao.class);
          LinkGroup found = groupDao.findById(dto.getId(), CurrentUser.getAccountId());

          if (found != null) {
            boolean isUpdated = 
              groupDao.update(
                dto.getId(),
                dto.getName(),
                dto.getPrice(),
                dto.getAccountId()
              );
            if (isUpdated) {
              // if base price is changed then all the prices and other 
              // indicators (on both group itself and its links) must be re-calculated accordingly
            	// Please note: no need to check any alarm since it is a user update!
              if (found.getPrice().doubleValue() != dto.getPrice().doubleValue()) {
          			CommonDao commonDao = handle.attach(CommonDao.class);
          			GroupRefreshResult grr = GroupRefreshResultConverter.convert(commonDao.refreshGroup(dto.getId()));

                //for returning data!
          			found.setLevel(grr.getLevel());
          			found.setTotal(grr.getTotal());
          			found.setMinPrice(grr.getMinPrice());
          			found.setAvgPrice(grr.getAvgPrice());
          			found.setMaxPrice(grr.getMaxPrice());
              }
              //for returning data!
              found.setName(dto.getName());
              found.setPrice(dto.getPrice());
              
              Map<String, LinkGroup> data = new HashMap<>(1);
              data.put("group", found);
              response = new Response(data);
            }
          } else {
          	response = Responses.NotFound.GROUP;
          }

          if (response.isOK())
          	handle.commit();
          else
          	handle.rollback();
        }
        return response;

      } else {
        return new Response(problem);
      }
    }
    return Responses.Invalid.GROUP;
  }

  Response delete(Long id) {
  	Response response = Responses.Invalid.GROUP;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
      	GroupDao groupDao = handle.attach(GroupDao.class);
      	LinkGroup group = groupDao.findById(id, CurrentUser.getAccountId());
      	
      	if (group != null) {
        	handle.begin();
      			
    			final String where = String.format("where group_id=%d and account_id=%d", id, CurrentUser.getAccountId());
          Batch batch = handle.createBatch();
          batch.add("SET FOREIGN_KEY_CHECKS=0");
          batch.add("delete from alarm " + where);
          batch.add("delete from link_price " + where);
          batch.add("delete from link_history " + where);
          batch.add("delete from link_spec " + where);
          batch.add("delete from link " + where);
          batch.add("delete from link_group " + where.replace("group_", "")); //this query determines the success!
          batch.add(
        		String.format(
      				"update account set link_count=link_count-%d where id=%d", 
      				group.getLinkCount(), CurrentUser.getAccountId()
    				)
      		);
          batch.add("SET FOREIGN_KEY_CHECKS=1");
          int[] result = batch.execute();

          if (result[6] > 0) {
            Map<String, Object> data = new HashMap<>(1);
            data.put("count", group.getLinkCount());
            response = new Response(data);
            handle.commit();
          } else {
          	handle.rollback();
      		}
      	}
      }
    }

    return response;
  }

  Response bulkInsert(LinkBulkInsertDTO dto) {
    Response response = validate(dto);

    if (response.isOK()) {
      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        AccountDao accountDao = handle.attach(AccountDao.class);
        GroupDao groupDao = handle.attach(GroupDao.class);
    		LinkDao linkDao = handle.attach(LinkDao.class);
    		
    		Set<String> urlList = null;

        Account account = accountDao.findById(CurrentUser.getAccountId());
        if (account.getPlan() != null) {
          int allowedLinkCount = (account.getPlan().getLinkLimit() - account.getLinkCount());
          urlList = response.getData();
          urlList.remove("");
  
          if (allowedLinkCount > 0) {
          	if (urlList.size() <= 100 && urlList.size() <= allowedLinkCount) {
  
          		for (Iterator<String> it = urlList.iterator(); it.hasNext();) {
  							Link link = new Link();
  							link.setUrl(it.next());
  							link.setUrlHash(DigestUtils.md5Hex(link.getUrl()));
  							link.setGroupId(dto.getGroupId());
  							link.setAccountId(CurrentUser.getAccountId());
  
  							long id = linkDao.insert(link);
  
  							link.setId(id);
  							link.setStatus(LinkStatus.TOBE_CLASSIFIED);
  							linkDao.insertHistory(link);
  						}
          		groupDao.increaseWaitingsCount(dto.getGroupId(), urlList.size());
  
          		response = Responses.OK;
  
            } else {
            	if (urlList.size() > 100) {
            		response = Responses.NotAllowed.LINK_LIMIT_EXCEEDED;
            	} else if (urlList.size() > allowedLinkCount) {
            		response = new Response("You can add max " + allowedLinkCount + " links more!");
            	}
            }
          } else {
            response = Responses.NotAllowed.HAVE_NO_PLAN;
          }
        } else {
          response = Responses.NotAllowed.NO_LINK_LIMIT;
        }

        if (response.isOK()) {
        	LinkGroup group = groupDao.findById(dto.getGroupId(), CurrentUser.getAccountId());

        	accountDao.increaseLinkCount(CurrentUser.getAccountId(), urlList.size());
        	int accountLinkCount = account.getLinkCount() + urlList.size();

          Map<String, Object> data = new HashMap<>(4);
        	data.put("group", group);
        	data.put("count", urlList.size());
        	data.put("linkCount", accountLinkCount);
          if (!dto.getFromSearchPage()) {
          	data.put("links", linkDao.findListByGroupId(dto.getGroupId(), CurrentUser.getAccountId()));
          }
          response = new Response(data);
        }

        if (response.isOK())
        	handle.commit();
        else
        	handle.rollback();
      } catch (Exception e) {
        log.error("Failed to import URL list!", e);
        response = Responses.ServerProblem.EXCEPTION;
      }
    }

    return response;
  }
  
  private String validate(GroupDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getName())) {
      problem = "Name cannot be empty!";
    } else if (dto.getName().length() < 3 || dto.getName().length() > 500) {
      problem = "Name must be between 3 and 128 chars!";
    }
    
    if (problem == null) {
    	if (dto.getPrice() != null && (dto.getPrice().compareTo(BigDecimal.ZERO) < 0 || dto.getPrice().compareTo(new BigDecimal(9_999_999)) > 0)) {
    		problem = "Price is out of reasonable range!";
    	}
    }

    if (problem == null) {
      dto.setAccountId(CurrentUser.getAccountId());
      dto.setName(SqlHelper.clear(dto.getName()));
    }

    return problem;
  }
  
  private Response validate(LinkBulkInsertDTO dto) {
    Response res = null;

  	if (dto == null) res = Responses.Invalid.DATA;
  	if (res == null && (dto.getGroupId() == null || dto.getGroupId() <= 0)) res = Responses.Invalid.GROUP;
    if (res == null && StringUtils.isBlank(dto.getLinksText())) res = Responses.NotSuitable.EMPTY_URL_LIST;

    Set<String> urlList = null;
    if (res == null) {
      String[] tempRows = dto.getLinksText().split("\n");
      if (tempRows.length < 1) {
      	res = Responses.NotSuitable.EMPTY_URL_LIST;
      }

      if (res == null) {
        urlList = new LinkedHashSet<>();
        List<String> problemList = new ArrayList<>();
    
        for (int i = 0; i < tempRows.length; i++) {
    			String row = tempRows[i];
    			if (StringUtils.isBlank(row)) continue;
    			if (URLUtils.isAValidURL(row)) {
    				urlList.add(row);
    			} else {
    				problemList.add(Integer.toString(i+1));
    			}
    		}
        if (problemList.size() > 0) {
        	if (problemList.size() >= urlList.size()/2) {
        		res = new Response("Mostly invalid URLs!");
        	} else {
        		res = new Response("Invalid URLs at " + String.join(", ", problemList));
        	}
        }
      }
    }

    if (res != null) return res; 
  	return new Response(urlList);
  }

}
