package io.inprice.api.app.product;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import io.inprice.api.app.product.dto.AddLinksDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.BaseSearchDTO;
import io.inprice.api.dto.ProductDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.info.ProductRefreshResult;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.Link;
import io.inprice.common.models.Product;
import io.inprice.common.utils.URLUtils;

class ProductService {

  private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
	
  Response findById(Long id) {
    try (Handle handle = Database.getHandle()) {
      ProductDao productDao = handle.attach(ProductDao.class);

      Product product = productDao.findByIdWithAlarm(id, CurrentUser.getAccountId());
      if (product != null) {
        return new Response(product);
      }
      return Responses.NotFound.PRODUCT;
    }
  }

  Response getIdNameList(Long excludedProductId) {
  	try (Handle handle = Database.getHandle()) {
  		ProductDao productDao = handle.attach(ProductDao.class);
  		return new Response(productDao.getIdNameList((excludedProductId != null ? excludedProductId : 0), CurrentUser.getAccountId()));
  	}
  }

  Response search(BaseSearchDTO dto) {
    try (Handle handle = Database.getHandle()) {
      ProductDao productDao = handle.attach(ProductDao.class);
      dto = DTOHelper.normalizeSearch(dto, true, true);
      dto.setTerm("%"+dto.getTerm());
      return new Response(productDao.search(dto));
    }
  }

  Response findLinksById(Long id) {
    try (Handle handle = Database.getHandle()) {
    	ProductDao productDao = handle.attach(ProductDao.class);
      LinkDao linkDao = handle.attach(LinkDao.class);

      Product product = productDao.findByIdWithAlarm(id, CurrentUser.getAccountId());
      if (product != null) {
      	Map<String, Object> dataMap = Map.of(
        	"product", product,
        	"links", linkDao.findListByProductId(id, CurrentUser.getAccountId())
      	);
        return new Response(dataMap);
      }
    }
    return Responses.NotFound.PRODUCT;
  }

  Response insert(ProductDTO dto) {
    String problem = validate(dto);
    if (problem == null) {
      try (Handle handle = Database.getHandle()) {
        ProductDao productDao = handle.attach(ProductDao.class);

        Product found = productDao.findByName(dto.getName(), CurrentUser.getAccountId());
        if (found == null) {
        	Long id = productDao.insert(dto);
        	if (id != null && id > 0) {
        		found = productDao.findById(id, CurrentUser.getAccountId());
            return new Response(Map.of("product", found));
          }
        } else {
        	return Responses.Already.Defined.PRODUCT;
        }
      }
      return Responses.DataProblem.DB_PROBLEM;
    } else {
      return new Response(problem);
    }
  }

  Response update(ProductDTO dto) {
  	Response res = Responses.NotFound.PRODUCT;

  	if (dto.getId() != null && dto.getId() > 0) {

      String problem = validate(dto);
      if (problem == null) {

        try (Handle handle = Database.getHandle()) {
          ProductDao productDao = handle.attach(ProductDao.class);

          //to prevent duplication, checking if any product other than this has the same name!
          Product found = productDao.findByName(dto.getName(), dto.getId(), CurrentUser.getAccountId());
          if (found == null) {

          	//must be found
          	found = productDao.findById(dto.getId(), CurrentUser.getAccountId());
            if (found != null) {
            	handle.begin();
            	
            	boolean isUpdated = productDao.update(dto);
  
              if (isUpdated) {
                // if base price is changed then all the prices and other 
                // indicators (on both product itself and its links) must be re-calculated accordingly
                if (found.getLinkCount() > 0 && found.getPrice().compareTo(dto.getPrice()) != 0) {

                	//refreshes product's totals and alarm if needed!
              		ProductRefreshResult PRR = ProductAlarmService.updateAlarm(dto.getId(), handle);
  
                  //for returning data!
            			found.setLevel(PRR.getLevel());
            			found.setTotal(PRR.getTotal());
            			found.setMinPrice(PRR.getMinPrice());
            			found.setAvgPrice(PRR.getAvgPrice());
            			found.setMaxPrice(PRR.getMaxPrice());
                }
                //for returning data!
                found.setName(dto.getName());
                found.setDescription(dto.getDescription());
                found.setPrice(dto.getPrice());
                
                res = new Response(Map.of("product", found));
              } else {
              	res = Responses.DataProblem.DB_PROBLEM;
              }

              if (res.isOK())
              	handle.commit();
              else
              	handle.rollback();
            }
          } else {
          	res = Responses.Already.Defined.PRODUCT;
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
  	Response res = Responses.Invalid.PRODUCT;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
      	ProductDao productDao = handle.attach(ProductDao.class);
      	Product product = productDao.findById(id, CurrentUser.getAccountId());
      	
      	if (product != null) {
        	handle.begin();
      			
    			String where = String.format("where product_id=%d and account_id=%d", id, CurrentUser.getAccountId());
          Batch batch = handle.createBatch();
          batch.add("SET FOREIGN_KEY_CHECKS=0");
          batch.add("delete from alarm " + where);
          batch.add("delete from link_price " + where);
          batch.add("delete from link_history " + where);
          batch.add("delete from link_spec " + where);
          batch.add("delete from link " + where);
          batch.add("delete from product " + where.replace("product_", "")); //this query determines the success!
          batch.add(
        		String.format(
      				"update account set link_count=link_count-%d where id=%d", 
      				product.getLinkCount(), CurrentUser.getAccountId()
    				)
      		);
          batch.add("SET FOREIGN_KEY_CHECKS=1");

          int[] result = batch.execute();

          if (result[6] > 0) {
            res = new Response(Map.of("count", product.getLinkCount()));
            handle.commit();
          } else {
          	handle.rollback();
      		}
      	} else {
      		res = Responses.NotFound.PRODUCT;
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
        ProductDao productDao = handle.attach(ProductDao.class);
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
  							link.setProductId(dto.getProductId());
  							link.setAccountId(CurrentUser.getAccountId());

  							long id = linkDao.insert(link);

  							link.setId(id);
  							link.setStatus(LinkStatus.TOBE_CLASSIFIED);
  							linkDao.insertHistory(link);
            	});

            	productDao.increaseWaitingsCount(dto.getProductId(), urlList.size());
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
        	Product product = productDao.findByIdWithAlarm(dto.getProductId(), CurrentUser.getAccountId());

        	int accountLinkCount = account.getLinkCount() + urlList.size();

          Map<String, Object> data = Map.of(
        		"product", product,
        		"count", urlList.size(),
        		"linkCount", accountLinkCount,
        		"links", linkDao.findListByProductId(dto.getProductId(), CurrentUser.getAccountId())
    			);
          res = new Response(data);
        }

      } catch (Exception e) {
        logger.error("Failed to import URL list!", e);
        res = Responses.ServerProblem.EXCEPTION;
      }
    }

    return res;
  }
  
  private String validate(ProductDTO dto) {
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

  	if (dto.getProductId() != null && dto.getProductId() > 0) {

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
  		res = Responses.NotFound.PRODUCT;
  	}

  	return res;
  }

}
