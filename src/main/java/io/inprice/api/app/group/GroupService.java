package io.inprice.api.app.group;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import io.inprice.api.dto.GroupDTO;
import io.inprice.api.dto.LinkDTO;
import io.inprice.api.dto.LinkMoveDTO;
import io.inprice.api.dto.LinkBulkInsertDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.models.Account;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkGroup;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.models.LinkPrice;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.repository.CommonRepository;
import io.inprice.common.utils.URLUtils;

class GroupService {

  private static final Logger log = LoggerFactory.getLogger(GroupService.class);
	
  Response findById(Long id) {
    try (Handle handle = Database.getHandle()) {
      GroupDao groupDao = handle.attach(GroupDao.class);

      LinkGroup group = groupDao.findById(id, CurrentUser.getAccountId());
      if (group != null) {
        return new Response(group);
      }
      return Responses.NotFound.GROUP;
    }
  }

  Response search(String term) {
    try (Handle handle = Database.getHandle()) {
      GroupDao groupDao = handle.attach(GroupDao.class);
      List<LinkGroup> list = null;
      if (StringUtils.isNotBlank(term)) {
      	list = groupDao.search(SqlHelper.clear(term) + "%", CurrentUser.getAccountId());
      } else {
      	list = groupDao.getList(CurrentUser.getAccountId());
      }
    	return new Response(list);
    }
  }

  Response findLinksById(Long id) {
    Map<String, Object> dataMap = new HashMap<>(2);

    try (Handle handle = Database.getHandle()) {
      GroupDao groupDao = handle.attach(GroupDao.class);

      LinkGroup group = groupDao.findById(id, CurrentUser.getAccountId());
      if (group != null) {
        dataMap.put("group", group);

        LinkDao linkDao = handle.attach(LinkDao.class);

        //finding links
        List<Link> linkList = linkDao.findListByGroupId(id, CurrentUser.getAccountId());
        if (linkList != null && linkList.size() > 0) {
          int i = 0;
          Map<Long, Integer> refListToLinks = new LinkedHashMap<>(linkList.size());
          for (Link link: linkList) {
            link.setPriceList(new ArrayList<>());
            link.setSpecList(new ArrayList<>());
            link.setHistoryList(new ArrayList<>());
            refListToLinks.put(link.getId(), i++);
          }

          List<LinkSpec> specsList = linkDao.findSpecListByGroupId(id); // finding links' specs
          List<LinkPrice> pricesList = linkDao.findPriceListByGroupId(id); // finding links' prices
          List<LinkHistory> historiesList = linkDao.findHistoryListByGroupId(id); // finding links' histories
          for (LinkSpec linkSpec: specsList) linkList.get(refListToLinks.get(linkSpec.getLinkId())).getSpecList().add(linkSpec);
          for (LinkPrice linkPrice: pricesList) linkList.get(refListToLinks.get(linkPrice.getLinkId())).getPriceList().add(linkPrice);
          for (LinkHistory linkHistory: historiesList) linkList.get(refListToLinks.get(linkHistory.getLinkId())).getHistoryList().add(linkHistory);
        }

        if (linkList != null) {
          dataMap.put("links", linkList);
        }

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
              return Responses.OK;
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

        final Response[] res = { Responses.DataProblem.DB_PROBLEM };

        try (Handle handle = Database.getHandle()) {
          handle.inTransaction(transaction -> {
            GroupDao groupDao = transaction.attach(GroupDao.class);

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
                if (found.getPrice().doubleValue() != dto.getPrice().doubleValue()) {
                  CommonRepository.refreshGroup(transaction, dto.getId());
                }
                res[0] = Responses.OK;
              }
            } else {
              res[0] = Responses.NotFound.GROUP;
            }

            return res[0].isOK();
          });
        }
        return res[0];

      } else {
        return new Response(problem);
      }
    }
    return Responses.Invalid.GROUP;
  }

  /**
   * Moves links from one to another.
   * Two operation is enough; a) setting new group id for all the selected links and b) refreshing group numbers
   * 
   */
  Response moveLinks(LinkMoveDTO dto) {
  	if (dto != null && dto.getToGroupId() != null && dto.getToGroupId() > 0) {
      if (dto.getLinkIdList() != null && dto.getLinkIdList().size() > 0) {

      	try (Handle handle = Database.getHandle()) {
        	handle.inTransaction(transaction -> {
            LinkDao linkDao = transaction.attach(LinkDao.class);
  
          	List<Long> groupIdList = linkDao.findGroupIdList(dto.getLinkIdList(), CurrentUser.getAccountId());
          	if (groupIdList != null && groupIdList.size() > 0) {
          		long affected = linkDao.changeGroupId(dto.getLinkIdList(), dto.getToGroupId());
          		if (affected == dto.getLinkIdList().size()) {
            		groupIdList.add(dto.getToGroupId());
            		for (Long groupId: groupIdList) {
            			CommonRepository.refreshGroup(transaction, groupId);
            		}
          		}
            }
            return true;
        	});
      	}
      	return Responses.OK;
      }
    }
  	return Responses.Invalid.DATA;
  }

  Response delete(Long id) {
    if (id != null && id > 0) {

      final int[] counts = { 0, 0 };
      final boolean[] isOK = { false };

      try (Handle handle = Database.getHandle()) {
      	
      	GroupDao groupDao = handle.attach(GroupDao.class);
      	LinkGroup group = groupDao.findById(id, CurrentUser.getAccountId());
      	
      	if (group != null) {
      			
    			if (group.getLinkCount() > 0) {
      			final String where = String.format("where group_id=%d and account_id=%d", id, CurrentUser.getAccountId());
            handle.inTransaction(transaction -> {
              Batch batch = transaction.createBatch();
              batch.add("delete from link_price " + where);
              batch.add("delete from link_history " + where);
              batch.add("delete from link_spec " + where);
              batch.add("delete from link_group " + where.replace("group_", "")); //this determines the success!
              batch.add("delete from link " + where);
              batch.add(
            		String.format(
          				"update account set link_count=link_count-%d where id=%d", 
          				group.getLinkCount(), CurrentUser.getAccountId()
        				)
          		);
              int[] result = batch.execute();
              isOK[0] = result[3] > 0;
              if (isOK[0]) {
              	int linkCount = transaction.attach(AccountDao.class).findLinkCount(CurrentUser.getAccountId());
              	counts[0] = linkCount;
              	counts[1] = group.getLinkCount();
              }
              return isOK[0]; //for the most inner code block (not for the method!)
            });
    			} else {
    				isOK[0] = groupDao.delete(id, CurrentUser.getAccountId());
    			}
      	}
      }

      if (isOK[0]) {
        Map<String, Object> data = new HashMap<>(2);
        data.put("count", counts[0]);
        data.put("linkCount", counts[1]);
        return new Response(data);
      }
    }
    return Responses.Invalid.GROUP;
  }

  Response bulkInsert(LinkBulkInsertDTO dto) {
    Response[] res = { validate(dto) };

    if (res[0].isOK()) {
      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transaction -> {
          AccountDao accountDao = transaction.attach(AccountDao.class);
  
          Account account = accountDao.findById(CurrentUser.getAccountId());
          int allowedLinkCount = (account.getLinkLimit() - account.getLinkCount());
          Set<String> urlList = res[0].getData();
  
          if (allowedLinkCount > 0) {
          	if (urlList.size() <= 100 && urlList.size() <= allowedLinkCount) {
  
          		LinkDao linkDao = transaction.attach(LinkDao.class);
          		List<LinkDTO> linkList = new ArrayList<>(urlList.size());
          		for (Iterator<String> it = urlList.iterator(); it.hasNext();) {
  							LinkDTO link = new LinkDTO();
  							link.setUrl(it.next());
  							link.setUrlHash(DigestUtils.md5Hex(link.getUrl()));
  							link.setGroupId(dto.getGroupId());
  							link.setAccountId(CurrentUser.getAccountId());
  							linkList.add(link);
  						}
          		linkDao.bulkInsert(linkList);
          		
          		GroupDao groupDao = transaction.attach(GroupDao.class);
          		groupDao.increaseWaitingsCount(dto.getGroupId(), urlList.size());

          		res[0] = Responses.OK;
  
            } else {
            	if (urlList.size() > 100) {
            		res[0] = Responses.NotAllowed.LINK_LIMIT_EXCEEDED;
            	} else if (urlList.size() > allowedLinkCount) {
            		res[0] = new Response("You can add max " + allowedLinkCount + " links more!");
            	}
            }
          } else {
            res[0] = Responses.NotAllowed.NO_LINK_LIMIT;
          }
  
          if (res[0].isOK()) {
            boolean isOK = accountDao.changeLinkCount(dto.getGroupId(), urlList.size());
            if (isOK) {
            	int linkCount = accountDao.findLinkCount(CurrentUser.getAccountId());
              Map<String, Object> data = new HashMap<>(2);
              data.put("count", urlList.size());
              data.put("linkCount", linkCount);
              res[0] = new Response(data);
            }
          }
  
          return res[0].isOK(); //not for method but the closest transaction block!
        });
      } catch (Exception e) {
        log.error("Failed to import URL list!", e);
        res[0] = Responses.ServerProblem.EXCEPTION;
      }
    }

    return res[0];
  }
  
  private String validate(GroupDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getName())) {
      problem = "Name cannot be null!";
    } else if (dto.getName().length() < 3 || dto.getName().length() > 500) {
      problem = "Name must be between 3 and 128 chars!";
    }
    
    if (problem == null) {
    	if (dto.getPrice() != null && (dto.getPrice().compareTo(BigDecimal.ONE) < 0 || dto.getPrice().compareTo(new BigDecimal(9_999_999)) > 0)) {
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
