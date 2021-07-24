package io.inprice.api.app.group;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
import io.inprice.api.app.group.dto.AddLinksDTO;
import io.inprice.api.app.link.LinkDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.GroupDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.info.GroupRefreshResult;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkGroup;
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

  Response getIdNameList(Long excludedGroupId) {
  	try (Handle handle = Database.getHandle()) {
  		GroupDao groupDao = handle.attach(GroupDao.class);
  		return new Response(groupDao.getIdNameList((excludedGroupId != null ? excludedGroupId : 0), CurrentUser.getAccountId()));
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
    String problem = validate(dto);
    if (problem == null) {
      try (Handle handle = Database.getHandle()) {
        GroupDao groupDao = handle.attach(GroupDao.class);

        LinkGroup found = groupDao.findByName(dto.getName(), CurrentUser.getAccountId());
        if (found == null) {
        	Long id = groupDao.insert(dto);
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

  Response update(GroupDTO dto) {
  	Response res = Responses.NotFound.GROUP;

  	if (dto.getId() != null && dto.getId() > 0) {

      String problem = validate(dto);
      if (problem == null) {

        try (Handle handle = Database.getHandle()) {
          GroupDao groupDao = handle.attach(GroupDao.class);

          //to prevent duplication, checking if any group other than this has the same name!
          LinkGroup found = groupDao.findByName(dto.getName(), dto.getId(), CurrentUser.getAccountId());
          if (found == null) {

          	//must be found
          	found = groupDao.findById(dto.getId(), CurrentUser.getAccountId());
            if (found != null) {
            	handle.begin();
            	
            	boolean isUpdated = groupDao.update(dto);
  
              if (isUpdated) {
                // if base price is changed then all the prices and other 
                // indicators (on both group itself and its links) must be re-calculated accordingly
                if (found.getPrice().compareTo(dto.getPrice()) != 0) {
            			
                	//refreshes group's totals and alarm if needed!
              		GroupRefreshResult grr = GroupAlarmService.updateAlarm(dto.getId(), handle);
  
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
                res = new Response(data);
              } else {
              	res = Responses.DataProblem.DB_PROBLEM;
              }

              if (res.isOK())
              	handle.commit();
              else
              	handle.rollback();
            }
          } else {
          	res = Responses.Already.Defined.GROUP;
          }
        }
        return res;

      } else {
      	res = new Response(problem);
      }
    }

  	return res;
  }

  Response delete(Long id) {
  	Response res = Responses.Invalid.GROUP;

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
            res = new Response(data);
            handle.commit();
          } else {
          	handle.rollback();
      		}
      	} else {
      		res = Responses.NotFound.GROUP;
      	}
      }
    }

    return res;
  }

  Response addLinks(AddLinksDTO dto) {
    Response res = validate(dto);

    if (res.isOK()) {
      try (Handle handle = Database.getHandle()) {
        AccountDao accountDao = handle.attach(AccountDao.class);
        GroupDao groupDao = handle.attach(GroupDao.class);
    		LinkDao linkDao = handle.attach(LinkDao.class);
    		
    		Set<String> urlList = null;

        Account account = accountDao.findById(CurrentUser.getAccountId());
        if (account.getPlan() != null) {
          int allowedLinkCount = (account.getPlan().getLinkLimit() - account.getLinkCount());
          urlList = res.getData();

          if (allowedLinkCount > 0) {
          	if (urlList.size() <= 100 && urlList.size() <= allowedLinkCount) {
  
            	handle.begin();
          		
            	urlList.forEach(url -> {
  							Link link = new Link();
  							link.setUrl(url);
  							link.setUrlHash(DigestUtils.md5Hex(url));
  							link.setGroupId(dto.getGroupId());
  							link.setAccountId(CurrentUser.getAccountId());

  							long id = linkDao.insert(link);

  							link.setId(id);
  							link.setStatus(LinkStatus.TOBE_CLASSIFIED);
  							linkDao.insertHistory(link);
            	});

            	groupDao.increaseWaitingsCount(dto.getGroupId(), urlList.size());
            	accountDao.increaseLinkCount(CurrentUser.getAccountId(), urlList.size());

            	handle.commit();
            	
          		res = Responses.OK;
  
            } else {
            	if (urlList.size() > 100) {
            		res = Responses.NotAllowed.LINK_LIMIT_EXCEEDED;
            	} else if (urlList.size() > allowedLinkCount) {
            		res = new Response("You can add up to " + allowedLinkCount + " link(s)!");
            	}
            }
          } else {
            res = Responses.NotAllowed.NO_LINK_LIMIT;
          }
        } else {
          res = Responses.NotAllowed.HAVE_NO_PLAN;
        }

        if (res.isOK()) {
        	LinkGroup group = groupDao.findByIdWithAlarm(dto.getGroupId(), CurrentUser.getAccountId());

        	int accountLinkCount = account.getLinkCount() + urlList.size();

          Map<String, Object> data = new HashMap<>(4);
        	data.put("group", group);
        	data.put("count", urlList.size());
        	data.put("linkCount", accountLinkCount);
        	data.put("links", linkDao.findListByGroupId(dto.getGroupId(), CurrentUser.getAccountId()));
          res = new Response(data);
        }

      } catch (Exception e) {
        log.error("Failed to import URL list!", e);
        res = Responses.ServerProblem.EXCEPTION;
      }
    }

    return res;
  }
  
  private String validate(GroupDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getName())) {
      problem = "Name cannot be empty!";
    } else if (dto.getName().length() < 3 || dto.getName().length() > 50) {
      problem = "Name must be between 3 - 50 chars!";
    }

    if (problem == null) {
    	if (StringUtils.isNotBlank(dto.getDescription()) && dto.getDescription().length() > 128) {
    		problem = "Description can be up to 128 chars!";
    	}
    }
    
    if (problem == null) {
    	if (dto.getPrice() != null && (dto.getPrice().compareTo(BigDecimal.ZERO) < 0 || dto.getPrice().compareTo(new BigDecimal(9_999_999)) > 0)) {
    		problem = "Price is out of reasonable range!";
    	}
    }

    if (problem == null) {
      dto.setAccountId(CurrentUser.getAccountId());
      dto.setName(SqlHelper.clear(dto.getName()));
      dto.setDescription(SqlHelper.clear(dto.getDescription()));
      if (dto.getPrice() == null) dto.setPrice(BigDecimal.ZERO);
    }

    return problem;
  }
  
  private Response validate(AddLinksDTO dto) {
    Response res = null;

  	if (dto.getGroupId() != null && dto.getGroupId() > 0) {

      if (StringUtils.isNotBlank(dto.getLinksText())) {

      	String[] tempRows = dto.getLinksText().split("\n");
        if (tempRows.length > 0) {
  
        	Set<String> urlList = new LinkedHashSet<>();
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

          if (problemList.size() == 0) {
          	res = new Response(urlList);
          } else {
          	if (problemList.size() > urlList.size()/2) {
          		res = new Response("Mostly invalid URLs!");
          	} else {
          		res = new Response("Invalid URL(s) at " + String.join(", ", problemList));
          	}
          }

        } else {
        	res = Responses.NotSuitable.EMPTY_URL_LIST;
        }
        
      } else {
      	res = Responses.NotSuitable.EMPTY_URL_LIST;
      }

  	} else {
  		res = Responses.NotFound.GROUP;
  	}

  	return res;
  }

}
