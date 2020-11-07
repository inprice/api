package io.inprice.api.app.product;

import org.jdbi.v3.core.Handle;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.app.product.dto.ProductDTO;
import io.inprice.api.app.tag.TagDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.validator.ProductValidator;

/**
 * This class is used by ProductService and Import services.
 */
public class ProductCreator {
  
  /**
   * Properly inserts a new product to the database
   * 
   * @param transactional must be in transactional
   * @param dto
   * @return
   */
  public static Response create(Handle transactional, ProductDTO dto) {
    Response res = Responses.Invalid.PRODUCT;

    if (dto != null) {

      String problem = ProductValidator.validate(dto);
      if (problem == null) {
        CompanyDao companyDao = transactional.attach(CompanyDao.class);
        ProductDao productDao = transactional.attach(ProductDao.class);

        dto.setCompanyId(CurrentUser.getCompanyId());

        boolean isIncreased = companyDao.increaseProductCountById(dto.getCompanyId());
        if (isIncreased) {
          Long id = 
            productDao.insert(
              dto.getCode(),
              dto.getName(),
              dto.getPrice(),
              dto.getCompanyId()
            );
          if (id != null && id > 0) {
            res = Responses.OK;
            if (dto.getTags() != null && dto.getTags().size() > 0) {
              TagDao tagDao = transactional.attach(TagDao.class);
              tagDao.insertTags(id, dto.getCompanyId(), dto.getTags());
            }
          } else {
            res = Responses.DataProblem.DUPLICATE;
          }
        } else {
          res = Responses.PermissionProblem.PRODUCT_LIMIT_PROBLEM;
        }
      } else {
        return new Response(problem);
      }
    }

    return res;
  }

}
