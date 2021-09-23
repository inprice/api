package io.inprice.api.app.definitions.brand;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.models.Brand;

public class BrandService {

  Response search(String value) {
    try (Handle handle = Database.getHandle()) {
      BrandDao brandDao = handle.attach(BrandDao.class);
      if (value == null) value = "";
      value = SqlHelper.clear(value) + "%";
      return new Response(brandDao.search(value, CurrentUser.getWorkspaceId()));
    }
  }

  Response list() {
    try (Handle handle = Database.getHandle()) {
      BrandDao brandDao = handle.attach(BrandDao.class);
      return new Response(brandDao.list(CurrentUser.getWorkspaceId()));
    }
  }

  Response insert(String value) {
  	Response res = Responses.DataProblem.DB_PROBLEM;
  	
    String problem = validate(value);
    if (problem == null) {
      try (Handle handle = Database.getHandle()) {
        BrandDao brandDao = handle.attach(BrandDao.class);

        String name = SqlHelper.clear(value);
        Brand found = brandDao.findByName(name, CurrentUser.getWorkspaceId());

        if (found == null) {
        	Long id = brandDao.insert(name, CurrentUser.getWorkspaceId());
        	if (id != null && id > 0) {
        		res = Responses.OK;
          }
        } else {
        	res = Responses.Already.Defined.BRAND;
        }
      }
    } else {
    	res = new Response(problem);
    }

    return res;
  }

  Response update(IdTextDTO dto) {
  	Response res = Responses.NotFound.BRAND;

  	if (dto.getId() != null && dto.getId() > 0) {
      String problem = validate(dto.getText());
      if (problem == null) {

        String name = SqlHelper.clear(dto.getText());
      	
        try (Handle handle = Database.getHandle()) {
          BrandDao brandDao = handle.attach(BrandDao.class);

          //to prevent duplication, checking if any brand other than this has the same name!
          Brand found = brandDao.findByName(name, dto.getId(), CurrentUser.getWorkspaceId());
          if (found == null) {
          	boolean isUpdated = brandDao.update(dto.getId(), name, CurrentUser.getWorkspaceId());
            if (isUpdated) res = Responses.OK;
          } else {
          	res = Responses.Already.Defined.BRAND;
          }
        }
      } else {
      	res = new Response(problem);
      }
  	}

  	return res;
  }

  Response delete(Long id) {
  	Response res = Responses.NotFound.BRAND;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
      	BrandDao brandDao = handle.attach(BrandDao.class);
      	Brand brand = brandDao.findById(id, CurrentUser.getWorkspaceId());

      	if (brand != null) {
        	handle.begin();
        	
        	brandDao.releaseProducts(id, CurrentUser.getWorkspaceId());
        	boolean isOK = brandDao.delete(id, CurrentUser.getWorkspaceId());

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
  
  private String validate(String name) {
    String problem = null;

    if (StringUtils.isBlank(name)) {
      problem = "Name cannot be empty!";
    } else if (name.length() < 2 || name.length() > 50) {
      problem = "Name must be between 2 - 50 chars!";
    }

    return problem;
  }

}
