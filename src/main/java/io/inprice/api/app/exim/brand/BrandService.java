package io.inprice.api.app.exim.brand;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.exim.EximBase;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;

public class BrandService extends EximBase {

  private static final Logger logger = LoggerFactory.getLogger(BrandService.class);

  Response upload(String csvContent) {
  	List<String> problems = new ArrayList<>();

    try (Handle handle = Database.getHandle()) {

    	BrandDao dao = handle.attach(BrandDao.class);
    	
    	int lineNumber = 1;
    	Set<String> looked = new HashSet<>();
    	Set<String> names = new HashSet<>();
    	
    	String[] rows = csvContent.lines().toArray(String[]::new);
      for (String row : rows) {
      	if (StringUtils.isBlank(row)) continue;

      	String name = row.trim();
      	if (name.charAt(0) == '"' && name.charAt(name.length()-1) == '"') name = name.substring(1, name.length()-1);
      	name = SqlHelper.clear(name);

      	if (looked.contains(name.toLowerCase())) {
      		problems.add(lineNumber + ". " + name  + " duplicate!");
    		} else {
	      	looked.add(name.toLowerCase()); //to check if it is already handled!

	      	if (name.length() < 2 || name.length() > 50) {
	          problems.add(lineNumber + ". must be between 2 - 50 chars!");
	        } else {
	        	boolean exist = dao.doesNameExist(name, CurrentUser.getWorkspaceId());
	        	if (exist) {
	        		problems.add(lineNumber + ". " + name  + " already exists!");
	        	} else {
	        		names.add(name);
	        	}
	        }
        }
      	if (lineNumber == 1000) break;
      	if (problems.size() >= 25) {
      		problems.add("Cancelled because there are too many problems in your file!");
      		names.clear();
      		break;
      	}
      	lineNumber++;
      }

      if (names.size() > 0) {
      	handle.begin();
      	dao.insertAll(names, CurrentUser.getWorkspaceId());
      	handle.commit();
      }

      //inserting the records
      Map<String, Object> result = Map.of(
    		"total", rows.length,
      	"successCount", names.size(),
      	"problems", problems
  		);
      
      return new Response(result);
		}
  }

  Response download(OutputStream outputStream) {
    try (Handle handle = Database.getHandle()) {
    	BrandDao dao = handle.attach(BrandDao.class);
    	List<String> brands = dao.getList(CurrentUser.getWorkspaceId());
    	if (brands.size() > 0) {
    		StringBuilder lines = new StringBuilder();
    		for (String name: brands) {
    			lines.append(normalizeValue(name, true));
    		}
    		IOUtils.copy(IOUtils.toInputStream(lines.toString(), "UTF-8") , outputStream);
    		return Responses.OK;
    	} else {
    		return Responses.NotFound.BRAND;
    	}
    } catch (IOException e) {
			logger.error("Failed to export brands", e);
		}

    return Responses.ServerProblem.EXCEPTION;
  }

}
