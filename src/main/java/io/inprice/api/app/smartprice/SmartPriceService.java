package io.inprice.api.app.smartprice;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.formula.FormulaHelper;
import io.inprice.common.formula.SmartPriceDTO;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.models.SmartPrice;

public class SmartPriceService {

  Response search(String value) {
    try (Handle handle = Database.getHandle()) {
      SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
      if (value == null) value = "";
      value = SqlHelper.clear(value) + "%";
      return new Response(smartPriceDao.search(value, CurrentUser.getWorkspaceId()));
    }
  }

  Response list() {
    try (Handle handle = Database.getHandle()) {
      SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
      return new Response(smartPriceDao.list(CurrentUser.getWorkspaceId()));
    }
  }

  Response insert(SmartPriceDTO dto) {
  	Response res = Responses.DataProblem.DB_PROBLEM;
  	
    String problem = validate(dto);
    if (problem == null) {
      try (Handle handle = Database.getHandle()) {
        SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);

        SmartPrice found = smartPriceDao.findByName(dto.getName(), CurrentUser.getWorkspaceId());
        if (found == null) {
        	Long id = smartPriceDao.insert(dto, CurrentUser.getWorkspaceId());
        	if (id != null && id > 0) {
        		res = Responses.OK;
          }
        } else {
        	res = Responses.Already.Defined.SMART_PRICE;
        }
      }
    } else {
    	res = new Response(problem);
    }

    return res;
  }

  Response update(SmartPriceDTO dto) {
  	Response res = Responses.NotFound.SMART_PRICE;

  	if (dto.getId() != null && dto.getId() > 0) {
      String problem = validate(dto);
      if (problem == null) {

        try (Handle handle = Database.getHandle()) {
          SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);

          //to prevent duplication, checking if any smartPrice other than this has the same name!
          SmartPrice found = smartPriceDao.findByName(dto.getName(), dto.getId(), CurrentUser.getWorkspaceId());
          if (found == null) {
          	boolean isUpdated = smartPriceDao.update(dto, CurrentUser.getWorkspaceId());
            if (isUpdated) res = Responses.OK;
          } else {
          	res = Responses.Already.Defined.SMART_PRICE;
          }
        }
      } else {
      	res = new Response(problem);
      }
  	}

  	return res;
  }

  Response delete(Long id) {
  	Response res = Responses.NotFound.SMART_PRICE;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
      	SmartPriceDao smartPriceDao = handle.attach(SmartPriceDao.class);
      	SmartPrice smartPrice = smartPriceDao.findById(id, CurrentUser.getWorkspaceId());

      	if (smartPrice != null) {
        	handle.begin();
        	
        	smartPriceDao.releaseProducts(id, CurrentUser.getWorkspaceId());
        	boolean isOK = smartPriceDao.delete(id, CurrentUser.getWorkspaceId());

        	if (isOK) {
            handle.commit();
            res = Responses.OK;
          } else {
          	handle.rollback();
          	res = Responses.DataProblem.DB_PROBLEM;
      		}
      	}
      }
    }

    return res;
  }
  
  private String validate(SmartPriceDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getName())) {
      problem = "Name cannot be empty!";
    } else if (dto.getName().length() > 70) {
      problem = "Name can be up to 70 chars!";
    }

    if (problem == null && StringUtils.isBlank(dto.getFormula())) {
      problem = "Formula cannot be empty!";
    } else if (dto.getFormula().length() > 255) {
      problem = "Formula can be up to 255 chars!";
    }

    if (problem == null && StringUtils.isNotBlank(dto.getLowerLimitFormula()) && dto.getLowerLimitFormula().length() > 255) {
  		problem = "Lower Limit Formula can be up to 255 chars!";
    }

    if (problem == null && StringUtils.isNotBlank(dto.getUpperLimitFormula()) && dto.getUpperLimitFormula().length() > 255) {
  		problem = "Upper Limit Formula can be up to 255 chars!";
    }

    if (problem == null) {
    	problem = FormulaHelper.verify(dto);
    }
    
    if (problem == null) {
    	dto.setName(SqlHelper.clear(dto.getName()));
    }
    
    return problem;
  }

}
