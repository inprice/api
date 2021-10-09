package io.inprice.api.app.definitions.category;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.consts.Responses;
import io.inprice.api.dto.IdTextDTO;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.models.Category;

public class CategoryService {

  Response search(String value) {
    try (Handle handle = Database.getHandle()) {
      CategoryDao categoryDao = handle.attach(CategoryDao.class);
      if (value == null) value = "";
      value = SqlHelper.clear(value) + "%";
      return new Response(categoryDao.search(value, CurrentUser.getWorkspaceId()));
    }
  }

  Response list() {
    try (Handle handle = Database.getHandle()) {
      CategoryDao categoryDao = handle.attach(CategoryDao.class);
      return new Response(categoryDao.list(CurrentUser.getWorkspaceId()));
    }
  }

  Response insert(String value) {
  	Response res = Responses.DataProblem.DB_PROBLEM;
  	
    String problem = validate(value);
    if (problem == null) {
      try (Handle handle = Database.getHandle()) {
        CategoryDao categoryDao = handle.attach(CategoryDao.class);

        String name = SqlHelper.clear(value);
        Category found = categoryDao.findByName(name, CurrentUser.getWorkspaceId());

        if (found == null) {
        	Long id = categoryDao.insert(name, CurrentUser.getWorkspaceId());
        	if (id != null && id > 0) {
        		res = Responses.OK;
          }
        } else {
        	res = Responses.Already.Defined.CATEGORY;
        }
      }
    } else {
    	res = new Response(problem);
    }

    return res;
  }

  Response update(IdTextDTO dto) {
  	Response res = Responses.NotFound.CATEGORY;

  	if (dto.getId() != null && dto.getId() > 0) {
      String problem = validate(dto.getText());
      if (problem == null) {

        String name = SqlHelper.clear(dto.getText());
      	
        try (Handle handle = Database.getHandle()) {
          CategoryDao categoryDao = handle.attach(CategoryDao.class);

          //to prevent duplication, checking if any category other than this has the same name!
          Category found = categoryDao.findByName(name, dto.getId(), CurrentUser.getWorkspaceId());
          if (found == null) {
          	boolean isUpdated = categoryDao.update(dto.getId(), name, CurrentUser.getWorkspaceId());
            if (isUpdated) res = Responses.OK;
          } else {
          	res = Responses.Already.Defined.CATEGORY;
          }
        }
      } else {
      	res = new Response(problem);
      }
  	}

  	return res;
  }

  Response delete(Long id) {
  	Response res = Responses.NotFound.CATEGORY;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
      	CategoryDao categoryDao = handle.attach(CategoryDao.class);
      	Category category = categoryDao.findById(id, CurrentUser.getWorkspaceId());

      	if (category != null) {
        	handle.begin();
        	
        	categoryDao.releaseProducts(id, CurrentUser.getWorkspaceId());
        	boolean isOK = categoryDao.delete(id, CurrentUser.getWorkspaceId());

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
