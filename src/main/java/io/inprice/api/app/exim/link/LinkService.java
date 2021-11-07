package io.inprice.api.app.exim.link;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.exim.EximBase;
import io.inprice.api.app.exim.link.mapper.DownloadBean;
import io.inprice.api.app.exim.link.mapper.DownloadBeanMapper;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LinkDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.utils.StringHelper;
import io.inprice.common.utils.URLUtils;
import io.javalin.http.Context;

public class LinkService extends EximBase {

  private static final Logger logger = LoggerFactory.getLogger(LinkService.class);

  Response upload(String fileContent) {
  	List<String> problems = new ArrayList<>();

    try (Handle handle = Database.getHandle()) {

    	LinkDao dao = handle.attach(LinkDao.class);

    	//to prevent multiple lookups to db
    	Map<String, Long> productMap = dao.getProducts(CurrentUser.getWorkspaceId());

    	int lineNumber = 1;
    	Set<String> looked = new HashSet<>(); //by product id and # and url hash
    	List<LinkDTO> dtos = new ArrayList<>();
    	
    	String[] rows = fileContent.lines().toArray(String[]::new);
      for (String row : rows) {
      	if (StringUtils.isBlank(row)) continue;
      	
      	Response buildRes = buildLinkDTO(row, dao, productMap);
      	if (buildRes.isOK()) {
      		LinkDTO dto = buildRes.getData();

      		if (looked.contains(dto.getProductId() + "#" + dto.getUrlHash())) {
	      		problems.add(lineNumber + ". duplicate!");
	    		} else {
		      	looked.add(dto.getProductId() + "#" + dto.getUrlHash()); //for later controls to check if it is already handled!
	
	        	boolean exist = dao.doesUrlHashExist(dto);
	        	if (exist) {
	        		problems.add(lineNumber + ". already exists!");
	        	} else {
	        		dtos.add(dto);
	        	}
	        }
      	} else {
      		problems.add(lineNumber + ". " + buildRes.getReason());
      	}
      	if (lineNumber == 1000) break;
      	if (problems.size() >= 25) {
      		problems.add("Cancelled because there are too many problems in your file!");
      		dtos.clear();
      		break;
      	}
      	lineNumber++;
      }

      //inserting the records
      if (dtos.size() > 0) {
      	handle.begin();
      	dao.insertAll(dtos);
      	handle.commit();
      }

      Map<String, Object> result = Map.of(
    		"total", rows.length,
      	"successCount", dtos.size(),
      	"problems", problems
  		);
      
      return new Response(result);
		}
  }

  /**
   * Extract LinkDTO out of csv line
   * 
   * @param line - csv line
   * @param productMap - product lookup map 
   * 
   * @return LinkDTO
   */
  private Response buildLinkDTO(String line, LinkDao dao, Map<String, Long> productMap) {
  	String problem = null;

  	//product sku and url are mandatory columns 
  	List<String> columns = StringHelper.splitCSV(line);
  	if (columns.size() == 2) {
  		String productSku = SqlHelper.clear(columns.get(0));
  		if (StringUtils.isNotBlank(productSku)) {
  			if (productSku.length() >= 3 && productSku.length() <= 50) {
  				Long productId = productMap.get(productSku.toLowerCase());
  				if (productId != null) {
  		  		String url = columns.get(1).trim();
  		  		if (StringUtils.isNotBlank(url)) {
  		    		if (URLUtils.isAValidURL(url)) {
  		    	  	LinkDTO dto = new LinkDTO();
	  		    		dto.setUrl(url);
	  		    		dto.setUrlHash(DigestUtils.md5Hex(url));
	  		    		dto.setProductId(productId);
	  		    		dto.setWorkspaceId(CurrentUser.getWorkspaceId());
	  		    		return new Response(dto);
          		} else {
          			problem = "Invalid Url!";
  		    		}
        		} else {
        			problem = "Url cannot be empty!";
  		  		}
      		} else {
      			problem = "Undefined Sku!";
  				}
    		} else {
    			problem = "Sku must be between 3 and 50 chars long!";
  			}
  		} else {
  			problem = "Sku is empty!";
  		}

			return new Response(problem);
		} else {
			return Responses.Invalid.CSV_COLUMN_COUNT;
  	}
  }

  Response download(Context ctx) {
    try (Handle handle = Database.getHandle()) {
      //---------------------------------------------------
      //fetching the data
      //---------------------------------------------------
    	handle.registerRowMapper(new DownloadBeanMapper());
      List<DownloadBean> dDeans =
        handle.createQuery(
          "select p.sku, l.url from link as l " +
      		"inner join product as p on p.id = l.product_id " +
      		"left join platform as pl on pl.id = l.platform_id " +
      		generateWhereClause(ctx) +
      		" order by p.sku, l.url"
        )
      .mapTo(DownloadBean.class)
      .list();

    	if (dDeans.size() > 0) {
    		StringBuilder lines = new StringBuilder();
    		for (DownloadBean bean: dDeans) {
    			lines.append(normalizeValue(bean.getSku()));
    			lines.append(normalizeValue(bean.getUrl(), true));
    		}
    		IOUtils.copy(IOUtils.toInputStream(lines.toString(), "UTF-8") , ctx.res.getOutputStream());
    		return Responses.OK;
    	} else {
    		return Responses.NotFound.LINK;
    	}
    } catch (IOException e) {
			logger.error("Failed to export categories", e);
		}

    return Responses.ServerProblem.EXCEPTION;
  }

	String generateWhereClause(Context ctx) {
		StringBuilder sql = new StringBuilder();

    sql.append("where l.workspace_id = ");
    sql.append(CurrentUser.getWorkspaceId());

  	if (ctx.queryParam("sku") != null) {
  		sql.append(" and p.sku = '");
  		sql.append(SqlHelper.clear(ctx.queryParam("sku")));
  		sql.append("'");
  	}

  	if (ctx.queryParam("brand") != null) {
  		sql.append(" and l.brand = '");
  		sql.append(SqlHelper.clear(ctx.queryParam("brand")));
  		sql.append("'");
  	}

  	if (ctx.queryParam("seller") != null) {
  		sql.append(" and l.seller = '");
			sql.append(SqlHelper.clear(ctx.queryParam("seller")));
  		sql.append("'");
  	}

  	if (ctx.queryParam("platform") != null) {
  		sql.append(" and pl.name = '");
  		sql.append(SqlHelper.clear(ctx.queryParam("platform")));
  		sql.append("'");
  	}

  	if (ctx.queryParam("platform") != null) {
  		sql.append(" and pl.name = '");
  		sql.append(SqlHelper.clear(ctx.queryParam("platform")));
  		sql.append("'");
  	}
  	
  	if (ctx.queryParam("grups") != null) {
    	sql.append(
  			String.format(" and l.grup in (%s) ", StringHelper.join("'", ctx.queryParams("grups")))
			);
    }
  	
  	if (ctx.queryParam("positions") != null) {
    	sql.append(
  			String.format(" and l.position in (%s) ", StringHelper.join("'", ctx.queryParams("positions")))
			);
    }

    return sql.toString();
	}
	
}
