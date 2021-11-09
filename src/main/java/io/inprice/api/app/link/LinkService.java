package io.inprice.api.app.link;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.link.dto.SearchDTO;
import io.inprice.api.app.product.ProductDao;
import io.inprice.api.app.product.ProductPriceService;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LinkDeleteDTO;
import io.inprice.api.dto.LinkMoveDTO;
import io.inprice.api.info.Response;
import io.inprice.api.meta.AlarmStatus;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.DTOHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.models.LinkPrice;
import io.inprice.common.models.LinkSpec;
import io.inprice.common.models.Product;
import io.inprice.common.utils.StringHelper;

class LinkService {

  private static final Logger logger = LoggerFactory.getLogger(LinkService.class);

  Response search(SearchDTO dto) {
  	dto = DTOHelper.normalizeSearch(dto, true);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder where = new StringBuilder();

    where.append("where l.workspace_id = ");
    where.append(dto.getWorkspaceId());

    if (dto.getAlarmStatus() != null && AlarmStatus.ALL.equals(dto.getAlarmStatus()) == false) {
  		where.append(" and l.alarm_id is ");
    	if (AlarmStatus.ALARMED.equals(dto.getAlarmStatus())) {
    		where.append(" not ");
    	}
    	where.append(" null");
    }
    
    if (StringUtils.isNotBlank(dto.getTerm())) {
    	where.append(" and CONCAT(ifnull(l.name, ''), ifnull(l.sku, ''), ifnull(l.seller, ''), ifnull(l.brand, ''))");
      where.append(" like '%");
      where.append(dto.getTerm());
      where.append("%' ");
    }

    if (CollectionUtils.isNotEmpty(dto.getPositions())) {
    	where.append(
  			String.format(" and l.position in (%s) ", StringHelper.join("'", dto.getPositions()))
			);
    }

    if (CollectionUtils.isNotEmpty(dto.getStatuses())) {
    	where.append(
		    String.format(" and grup in (%s) ", StringHelper.join("'", dto.getStatuses()))
			);
    }

    //---------------------------------------------------
    //fetching the data
    //---------------------------------------------------
    try (Handle handle = Database.getHandle()) {
      List<Link> searchResult =
        handle.createQuery(
          "select * from link as l " + 
          where +
          " order by " + dto.getOrderBy().getFieldName() + dto.getOrderDir().getDir() + ", l.id " +
          " limit " + dto.getRowCount() + ", " + dto.getRowLimit()
        )
      .map(new LinkMapper())
      .list();
      
      return new Response(searchResult);
    } catch (Exception e) {
      logger.error("Failed in full search for links.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  Response delete(LinkDeleteDTO dto) {
  	Response response = Responses.NotFound.LINK;
  	
  	if (CollectionUtils.isNotEmpty(dto.getLinkIdSet())) dto.getLinkIdSet().remove(null);

    if (CollectionUtils.isNotEmpty(dto.getLinkIdSet())) {
    	int count = dto.getLinkIdSet().size();
    	
    	String joinedIds = StringUtils.join(dto.getLinkIdSet(), ",");
      String where = String.format("where link_id in (%s) and workspace_id=%d ", joinedIds, CurrentUser.getWorkspaceId());

      try (Handle handle = Database.getHandle()) {
      	handle.begin();

      	//by links, finding the products whose sums and alarms be refreshed
      	LinkDao linkDao = handle.attach(LinkDao.class);
      	Set<Long> productIdSet = linkDao.findProductIdSet(dto.getLinkIdSet());
      	
        Batch batch = handle.createBatch();
        batch.add("SET FOREIGN_KEY_CHECKS=0");
        batch.add("delete from alarm " + where);
        batch.add("delete from link_price " + where);
        batch.add("delete from link_history " + where);
        batch.add("delete from link_spec " + where);
        batch.add("delete from link " + where.replace("link_", "")); //this query determines the success!
				batch.add(
					String.format(
						"update workspace set link_count=link_count-%d where id=%d",
						count, CurrentUser.getWorkspaceId()
					)
				);
				batch.add("SET FOREIGN_KEY_CHECKS=1");

				int[] result = batch.execute();

        if (result[5] > 0) {
        	if (CollectionUtils.isNotEmpty(productIdSet)) {
        		ProductPriceService.refresh(productIdSet, handle);
        	}

          if (dto.getFromProductId() != null) { //meaning that it is called from product definition (not from links search page)
          	Product product = handle.attach(ProductDao.class).findByIdWithLookups(dto.getFromProductId(), CurrentUser.getWorkspaceId());
          	Map<String, Object> data = Map.of(
          		"product", product,
            	"links", linkDao.findListByProductId(dto.getFromProductId(), CurrentUser.getWorkspaceId())
        		);
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
   * Moves links from one product to another.
   * 
   * Two operations are done accordingly;
   * 	a) setting new product id for all the selected links 
   * 	b) refreshing product sums
   * 
   */
  Response moveTo(LinkMoveDTO dto) {
  	Response res = Responses.OK;
  	
  	if (dto.getToProductId() != null && dto.getToProductId() > 0) {
  		if (CollectionUtils.isNotEmpty(dto.getLinkIdSet())) dto.getLinkIdSet().remove(null);

      if (CollectionUtils.isNotEmpty(dto.getLinkIdSet())) {
      	try (Handle handle = Database.getHandle()) {
        	handle.begin();

      		LinkDao linkDao = handle.attach(LinkDao.class);
          
        	Set<Long> foundProductIdSet = linkDao.findProductIdSet(dto.getLinkIdSet());
        	if (CollectionUtils.isNotEmpty(foundProductIdSet)) foundProductIdSet.remove(null);

        	if (CollectionUtils.isNotEmpty(foundProductIdSet)) {
	    			String joinedIds = StringUtils.join(dto.getLinkIdSet(), ",");
	    			String 
	    				updatePart = 
	    					String.format(
	  							"set product_id=%d where link_id in (%s) and product_id!=%d and workspace_id=%d", 
	  							dto.getToProductId(), joinedIds, dto.getToProductId(), CurrentUser.getWorkspaceId()
							);

            Batch batch = handle.createBatch();
            batch.add("update alarm " + updatePart);
            batch.add("update link_price " + updatePart);
            batch.add("update link_history " + updatePart);
            batch.add("update link_spec " + updatePart);
            batch.add("update link " + updatePart.replace("link_", "")); //this query determines the success!
  					int[] result = batch.execute();

  					if (result[4] > 0) {
  						//refreshes product sums and alarm if needed!
          		foundProductIdSet.add(dto.getToProductId());
          		ProductPriceService.refresh(foundProductIdSet, handle);

          		if (dto.getFromProductId() != null) { //meaning that it is called from product definition (not from links searching page)
                ProductDao productDao = handle.attach(ProductDao.class);
              	Product product = productDao.findById(dto.getFromProductId(), CurrentUser.getWorkspaceId());

              	Map<String, Object> data = Map.of(
              		"product", product,
                	"links", linkDao.findListByProductId(dto.getFromProductId(), CurrentUser.getWorkspaceId())
            		);
                res = new Response(data);
          		} else {
          			res = Responses.OK;
          		}
            } else {
            	res = Responses.NotFound.LINK;
  					}
          } else {
          	res = Responses.NotFound.PRODUCT;
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
    	res = Responses.Invalid.PRODUCT;
    }

  	return res;
  }

  Response getDetails(Long id) {
    Response res = Responses.NotFound.LINK;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        LinkDao linkDao = handle.attach(LinkDao.class);

        Link link = linkDao.findWithAlarmById(id, CurrentUser.getWorkspaceId());
        if (link != null) {
          List<LinkSpec> specList = linkDao.findSpecListByLinkId(link.getId());
          List<LinkPrice> priceList = linkDao.findPriceListByLinkId(link.getId());
          List<LinkHistory> historyList = linkDao.findHistoryListByLinkId(link.getId());

          if (specList == null) specList = new ArrayList<>();
          if (priceList == null) priceList = new ArrayList<>();
          if (historyList == null) historyList = new ArrayList<>();
        
          if (StringUtils.isNotBlank(link.getBrand())) specList.add(0, new LinkSpec("", link.getBrand()));
          if (StringUtils.isNotBlank(link.getSku())) specList.add(0, new LinkSpec("", link.getSku()));
          
          Map<String, Object> data = Map.of(
        		"info", link,
          	"specList", specList,
          	"priceList", priceList,
          	"historyList", historyList
        	);
          res = new Response(data);
        }
      }
    }

    return res;
  }

}
