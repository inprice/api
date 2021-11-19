package io.inprice.api.app.product;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.brand.BrandDao;
import io.inprice.api.app.category.CategoryDao;
import io.inprice.api.app.link.LinkDao;
import io.inprice.api.app.product.dto.AddLinksDTO;
import io.inprice.api.app.product.dto.SearchDTO;
import io.inprice.api.app.smartprice.SmartPriceDao;
import io.inprice.api.app.workspace.WorkspaceDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.ProductDTO;
import io.inprice.api.info.Response;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.formula.EvaluationResult;
import io.inprice.common.formula.FormulaHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.info.ProductRefreshResult;
import io.inprice.common.mappers.ProductMapper;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Brand;
import io.inprice.common.models.Category;
import io.inprice.common.models.Link;
import io.inprice.common.models.Product;
import io.inprice.common.models.SmartPrice;
import io.inprice.common.models.Workspace;
import io.inprice.common.utils.StringHelper;
import io.inprice.common.utils.URLUtils;

class ProductService {

  private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
	
  Response findById(Long id) {
    try (Handle handle = Database.getHandle()) {
      ProductDao productDao = handle.attach(ProductDao.class);

      Product product = productDao.findByIdWithLookups(id, CurrentUser.getWorkspaceId());
      if (product != null) {
        return new Response(product);
      }
      return Responses.NotFound.PRODUCT;
    }
  }

  Response getIdNameList(Long excludedProductId) {
  	try (Handle handle = Database.getHandle()) {
  		ProductDao productDao = handle.attach(ProductDao.class);
  		return new Response(productDao.getIdNameList((excludedProductId != null ? excludedProductId : 0), CurrentUser.getWorkspaceId()));
  	}
  }

  Response search(SearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, true);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder where = new StringBuilder();

    where.append("where p.workspace_id = ");
    where.append(dto.getWorkspaceId());

    if (dto.getAlarmStatus() != null && AlarmStatus.ALL.equals(dto.getAlarmStatus()) == false) {
  		where.append(" and p.alarm_id is ");
    	if (AlarmStatus.ALARMED.equals(dto.getAlarmStatus())) {
    		where.append(" not ");
    	}
    	where.append(" null");
    }

    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and CONCAT(p.name, p.sku)");
      where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

    if (dto.getBrand() != null) {
  		where.append(" and p.brand_id = ");
  		where.append(dto.getBrand().getId());
    }

    if (dto.getCategory() != null) {
  		where.append(" and p.category_id = ");
  		where.append(dto.getCategory().getId());
    }

    if (CollectionUtils.isNotEmpty(dto.getPositions())) {
    	where.append(
  			String.format(" and p.position in (%s) ", StringHelper.join("'", dto.getPositions()))
			);
    }

    //---------------------------------------------------
    //fetching the data
    //---------------------------------------------------
    try (Handle handle = Database.getHandle()) {
      List<Product> searchResult =
        handle.createQuery(
          "select p.*, brn.name as brand_name, cat.name as category_name from product as p " +
      		"left join brand as brn on brn.id = p.brand_id " +
      		"left join category as cat on cat.id = p.category_id " +
          where +
          " order by " + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir() + ", p.id " +
          " limit " + dto.getRowCount() + ", " + dto.getRowLimit()
        )
      .map(new ProductMapper())
      .list();

			return new Response(searchResult);
    } catch (Exception e) {
      logger.error("Failed in full search for products.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  Response findLinksById(Long id) {
    try (Handle handle = Database.getHandle()) {
    	ProductDao productDao = handle.attach(ProductDao.class);
      LinkDao linkDao = handle.attach(LinkDao.class);

      Product product = productDao.findByIdWithLookups(id, CurrentUser.getWorkspaceId());
      if (product != null) {
      	Map<String, Object> dataMap = Map.of(
        	"product", product,
        	"links", linkDao.findListByProductId(id, CurrentUser.getWorkspaceId())
      	);
        return new Response(dataMap);
      }
    }
    return Responses.NotFound.PRODUCT;
  }

  Response insert(ProductDTO dto) {
    String problem = ProductVerifier.verify(dto);
    if (problem == null) {
      try (Handle handle = Database.getHandle()) {
        ProductDao productDao = handle.attach(ProductDao.class);

        dto.setId(0L); //necessary for the existency check here!
      	boolean alreadyExists = productDao.doesExistBySku(dto, CurrentUser.getWorkspaceId());

      	if (alreadyExists == false) {
        	checkBrand(dto, handle);
        	checkCategory(dto, handle);
        	checkSmartPrice(dto, handle);

        	Long id = productDao.insert(dto);
        	if (id != null && id > 0) {
        		return Responses.OK;
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

  		String problem = ProductVerifier.verify(dto);
      if (problem == null) {

        try (Handle handle = Database.getHandle()) {
          ProductDao productDao = handle.attach(ProductDao.class);

          //checks if sku is already used for another product
        	boolean alreadyExists = productDao.doesExistBySku(dto, CurrentUser.getWorkspaceId());

          if (alreadyExists == false) {
          	Product found = productDao.findById(dto.getId(), CurrentUser.getWorkspaceId());
            if (found != null) { //must be found
            	handle.begin();

            	checkBrand(dto, handle);
            	checkCategory(dto, handle);
            	checkSmartPrice(dto, handle);

            	String suggestedPricePart = null;
            	if (found.getActives() > 0) {
            		ProductRefreshResult prr = null;

            		if (found.getPrice().equals(dto.getPrice()) == false || found.getBasePrice().equals(dto.getBasePrice()) == false) {
            			//prices must be updated before price refreshing sp called!
            			handle.execute(
          					"update product set price=?, base_price=? where id=? and workspace_id=?",
              			dto.getPrice(), dto.getBasePrice(), dto.getId(), CurrentUser.getWorkspaceId()
            			);
            			prr = ProductPriceService.refresh(found, handle);
            		}

            		//if dto.getSmartPriceId() == null, suggested_price is zeroized in generateUpdateQuery method!
            		if (dto.getSmartPriceId() != null && Objects.equals(found.getSmartPriceId(), dto.getSmartPriceId()) == false) {
            			SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
            			SmartPrice smartPrice = smartPriceDao.findById(dto.getSmartPriceId(), CurrentUser.getWorkspaceId());
            			if (smartPrice != null) {
	            			if (prr == null) {
	               			prr = ProductPriceService.refresh(found, handle);
	               		}
										EvaluationResult result = FormulaHelper.evaluate(smartPrice, prr);
	                	suggestedPricePart =
		                    String.format(
		                      ", suggested_price=%f, suggested_price_problem=%s ",
		                      result.getValue(),
		                      (result.getProblem() != null ? "'"+result.getProblem()+"'" : "null"),
		                      dto.getId()
		                    );
            			} else {
            				dto.setSmartPriceId(null);
            			}
              	}
            	}
            	
            	//we need to take care of sums, alarm and suggested price 
            	//since because all these indicators are sensitive for the changings on price and smart_price_id!
            	String updateQuery = generateUpdateQuery(found, dto, suggestedPricePart);

            	int affected = handle.execute(
          			updateQuery,
          			dto.getSku(), dto.getName(), dto.getPrice(), dto.getBasePrice(), dto.getBrandId(), dto.getCategoryId(), dto.getId(), CurrentUser.getWorkspaceId()
        			);

              if (affected > 0) {
              	if (Objects.equals(dto.getFrom(), "SearchPage")) {
              		res = Responses.OK;
              	} else {
	              	found = productDao.findByIdWithLookups(dto.getId(), CurrentUser.getWorkspaceId());
	                res = new Response(Map.of("product", found));
              	}
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
  
  private String generateUpdateQuery(Product found, ProductDTO dto, String suggestedPricePart) {
  	StringBuilder query = new StringBuilder("update product set sku=?, name=?, price=?, base_price=?, brand_id=?, category_id=?");

  	if (dto.getSmartPriceId() != null) {
  		query.append(", smart_price_id=" + dto.getSmartPriceId());
  		if (suggestedPricePart != null) {
  			query.append(suggestedPricePart);
  		}
  	} else {
  		query.append(", smart_price_id=null, suggested_price=0, suggested_price_problem=null");
  	}

  	if (found.getActives() < 1) {
  		query.append(
				", min_price=0, min_diff=0, min_platform=null, min_seller=null, " +
				"max_price=0, max_diff=0, max_platform=null, max_seller=null, " +
				"avg_price=0, avg_diff=0, position='NotSet'"
			);
  	}

  	query.append(" where id=? and workspace_id=?");

  	return query.toString();
  }

  Response delete(Long id) {
  	Response res = Responses.Invalid.PRODUCT;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
      	ProductDao productDao = handle.attach(ProductDao.class);
      	Product product = productDao.findById(id, CurrentUser.getWorkspaceId());
      	
      	if (product != null) {
        	handle.begin();

    			String where = String.format("where product_id=%d and workspace_id=%d", id, CurrentUser.getWorkspaceId());
        	
          Batch batch = handle.createBatch();
          batch.add("SET FOREIGN_KEY_CHECKS=0");
          batch.add("delete from link_price " + where);
          batch.add("delete from link_history " + where);
          batch.add("delete from link_spec " + where);
          batch.add("delete from link " + where);
          batch.add("delete from product " + where.replace("product_", "")); //this query determines the success!
          batch.add(
        		String.format(
      				"update workspace set link_count=link_count-%d where id=%d", 
      				product.getLinkCount(), CurrentUser.getWorkspaceId()
    				)
      		);
          batch.add("SET FOREIGN_KEY_CHECKS=1");

          int[] result = batch.execute();

          if (result[5] > 0) {
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
  	Response res = Responses.Invalid.LINK;

		try (Handle handle = Database.getHandle()) {
    	LinkDao linkDao = handle.attach(LinkDao.class);

      res = validate(dto, linkDao);
      if (res.isOK()) {
	    	WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
	      ProductDao productDao = handle.attach(ProductDao.class);
	  		
	  		Set<String> urlList = null;
	
	      Workspace workspace = workspaceDao.findById(CurrentUser.getWorkspaceId());
	      if (workspace.getPlan() != null) {
	        int allowedLinkCount = (workspace.getPlan().getLinkLimit() - workspace.getLinkCount());
	        urlList = res.getData();
	
	        if (allowedLinkCount > 0) {
	        	if (urlList.size() <= 100 && urlList.size() <= allowedLinkCount) {
	
	          	handle.begin();
	        		
	          	urlList.forEach(url -> {
								Link link = new Link();
								link.setUrl(url);
								link.setUrlHash(DigestUtils.md5Hex(url));
								link.setProductId(dto.getProductId());
								link.setWorkspaceId(CurrentUser.getWorkspaceId());
	
								long id = linkDao.insert(link);
	
								link.setId(id);
								link.setStatus(LinkStatus.TOBE_CLASSIFIED);
								linkDao.insertHistory(link);
	          	});
	
	          	productDao.incWaitingsCount(dto.getProductId(), urlList.size());
	          	workspaceDao.incLinkCount(CurrentUser.getWorkspaceId(), urlList.size());
	
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
	      	Product product = productDao.findByIdWithLookups(dto.getProductId(), CurrentUser.getWorkspaceId());
	      	int workspaceLinkCount = workspace.getLinkCount() + urlList.size();
	
	        Map<String, Object> data = Map.of(
	      		"product", product,
	      		"count", urlList.size(),
	      		"linkCount", workspaceLinkCount,
	      		"links", linkDao.findListByProductId(dto.getProductId(), CurrentUser.getWorkspaceId())
	  			);
	        res = new Response(data);
	      }
      }
    } catch (Exception e) {
      logger.error("Failed to import URL list!", e);
      res = Responses.ServerProblem.EXCEPTION;
    }

    return res;
  }
  
  private Response validate(AddLinksDTO dto, LinkDao linkDao) {
    Response res = null;

  	if (dto.getProductId() != null && dto.getProductId() > 0) {

      if (StringUtils.isNotBlank(dto.getLinksText())) {

      	String[] tempRows = dto.getLinksText().split("\n");
        if (tempRows.length > 0) {
  
        	Set<String> urlList = new LinkedHashSet<>();
        	List<String> problemList = new ArrayList<>();
          List<String> dupliceList = new ArrayList<>();
      
          for (int i = 0; i < tempRows.length; i++) {
      			String row = tempRows[i];
      			if (StringUtils.isBlank(row)) continue;

      			if (URLUtils.isAValidURL(row)) {
      				if (linkDao.doesExistByUrl(row, dto.getProductId()) == false) {
      					urlList.add(row);
      				} else {
      					dupliceList.add(Integer.toString(i+1));
      				}
      			} else {
      				problemList.add(Integer.toString(i+1));
      			}
      		}

          if (problemList.size() + dupliceList.size() == 0) {
          	res = new Response(urlList);
          } else {
          	if (problemList.size() + dupliceList.size() > urlList.size()/2) {
          		res = new Response("Mostly invalid or duplicated URLs!");
          	} else {
          		res = new Response("Invalid URL(s) at " + String.join(", ", problemList) + (dupliceList.size() > 0 ? ". Duplice URL(s) at " + String.join(", ", dupliceList) : ""));
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

  private Brand checkBrand(ProductDTO dto, Handle handle) {
  	BrandDao brandDao = handle.attach(BrandDao.class);

  	Brand found = null;
  	if (dto.getBrand() != null) {
	  	if (dto.getBrand().getId() != null) {
	  		found = brandDao.findById(dto.getBrand().getId(), dto.getWorkspaceId());

	  	} else if (StringUtils.isNotBlank(dto.getBrand().getName())) {
				String name = SqlHelper.clear(dto.getBrand().getName());
				found = brandDao.findByName(name, dto.getWorkspaceId());
	  		if (found == null) {
	  			found = new Brand();
	  			found.setId(brandDao.insert(name, dto.getWorkspaceId()));
	  			found.setName(name);
	  		}
	  	}
  	}

  	if (found != null) dto.setBrandId(found.getId());
  	
  	return found;
  }

  private Category checkCategory(ProductDTO dto, Handle handle) {
		CategoryDao categoryDao = handle.attach(CategoryDao.class);

		Category found = null;
  	if (dto.getCategory() != null) {
  		if (dto.getCategory().getId() != null) {
	  		found = categoryDao.findById(dto.getCategory().getId(), dto.getWorkspaceId());
	
	  	} else if (StringUtils.isNotBlank(dto.getCategory().getName())) {
				String name = SqlHelper.clear(dto.getCategory().getName());
				found = categoryDao.findByName(name, dto.getWorkspaceId());
	  		if (found == null) {
	  			found = new Category();
	  			found.setId(categoryDao.insert(name, dto.getWorkspaceId()));
	  			found.setName(name);
	  		}
	  	}
  	}

  	if (found != null) dto.setCategoryId(found.getId());

  	return found;
  }

  private void checkSmartPrice(ProductDTO dto, Handle handle) {
  	if (dto.getSmartPriceId() != null) {
  		SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
    	SmartPrice found = smartPriceDao.findById(dto.getSmartPriceId(), dto.getWorkspaceId());
    	if (found == null) {
    		dto.setSmartPriceId(null);
    	}
  	}
  }

}
