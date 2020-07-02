package io.inprice.api.app.product_import;

import java.io.BufferedReader;
import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyRepository;
import io.inprice.api.app.competitor.CompetitorRepository;
import io.inprice.api.app.product.ProductRepository;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CompetitorDTO;
import io.inprice.api.external.Props;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.ImportType;
import io.inprice.common.models.Company;
import io.inprice.common.utils.URLUtils;

public class ProductGenericImportService implements IProductImportService {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);
  private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
  private static final CompetitorRepository linkRepository = Beans.getSingleton(CompetitorRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  private static final String ASIN_REGEX = "^(?i)(B0|BT)[0-9A-Z]{8}$";
  private static final String SKU_REGEX = "^[1-3][0-9]{10,11}$";

  public ServiceResponse upload(ImportType importType, String content) {
    ServiceResponse res = Responses.DataProblem.DB_PROBLEM;

    String identifier = null;
    String regex = null;

    switch (importType) {
      case EBAY_SKU: {
        identifier = "SKU";
        regex = SKU_REGEX;
        break;
      }
      case AMAZON_ASIN: {
        identifier = "ASIN";
        regex = ASIN_REGEX;
        break;
      }
      case URL: {
        identifier = "URL";
        regex = URLUtils.URL_CHECK_REGEX;
        break;
      }
      case CSV: {
        return Responses.PermissionProblem.UNAUTHORIZED;
      }
    }

    Connection con = null;

    try {
      con = db.getTransactionalConnection();
      Company company = companyRepository.findById(con, CurrentUser.getCompanyId());

      int allowedCount = company.getProductLimit() - company.getProductCount();
      if (allowedCount > 0) {

        int actualCount = company.getProductCount();
        if (actualCount < allowedCount) {

          Set<String> insertedCodeSet = new HashSet<>();
          List<Map<String, String>> resultMapList = new ArrayList<>();

          try (BufferedReader reader = new BufferedReader(new StringReader(content))) {

            String line;
            while ((line = reader.readLine()) != null) {
              if (StringUtils.isBlank(line) || line.trim().startsWith("#")) {
                continue;
              }

              String result = "";

              if (actualCount < allowedCount) {
                boolean exists = insertedCodeSet.contains(line);
                if (! exists) {
                  if (line.matches(regex)) {

                    ServiceResponse op = null;

                    if (ImportType.URL.equals(importType)) {
                      exists = linkRepository.doesExistByUrl(con, line, null);
                      if (exists) {
                        op = Responses.DataProblem.DUPLICATE;
                      } else {
                        op = Responses.NotFound.PRODUCT;
                      }
                    } else {
                      op = productRepository.findByCode(con, line);
                    }

                    if (op.equals(Responses.NotFound.PRODUCT)) {
                      CompetitorDTO link = new CompetitorDTO();
                      switch (importType) {
                        case EBAY_SKU: {
                          link.setUrl(Props.PREFIX_FOR_SEARCH_EBAY() + line);
                          break;
                        }
                        case AMAZON_ASIN: {
                          link.setUrl(Props.PREFIX_FOR_SEARCH_AMAZON() + line);
                          break;
                        }
                        default:
                          link.setUrl(line);
                          break;
                      }

                      op = linkRepository.insert(con, link);
                      if (op.isOK()) {
                        actualCount++;
                        insertedCodeSet.add(line);
                        result = "Added successfully and will be handled in a short while";
                      } else {
                        result = op.getReason();
                      }
                    } else {
                      result = "This is already defined!";
                    }
                  } else {
                    result = "Doesn't match the rules!";
                  }
                } else {
                  result = "This is already handled. Please see previous rows in the list!";
                }
              } else {
                result = "Your product count reached the limit of your plan. Allowed prod. count: " + allowedCount;
              }

              Map<String, String> resultMap = new HashMap<>(2);
              resultMap.put("line", line);
              resultMap.put("result", result);
              resultMapList.add(resultMap);
              if (result.indexOf("reached") > 0) break;
            }

            res = new ServiceResponse(resultMapList);
          }

        } else {
          res = new ServiceResponse("You have reached your plan's maximum product limit.");
        }

      } else {
        res = new ServiceResponse("You haven't chosen a plan yet. You need to buy a plan to be able to import your products.");
      }

      db.commit(con);

    } catch (Exception e) {
      if (con != null) {
        db.rollback(con);
      }
      log.error("Failed to import " + identifier + " list.", e);
      res = Responses.DataProblem.DB_PROBLEM;
    } finally {
      if (con != null) {
        db.close(con);
      }
    }

    return res;
  }

}
