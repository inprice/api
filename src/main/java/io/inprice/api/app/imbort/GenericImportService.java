package io.inprice.api.app.imbort;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.app.link.LinkDao;
import io.inprice.api.app.product.ProductDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.Props;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.ImportType;
import io.inprice.common.models.Company;
import io.inprice.common.models.Link;
import io.inprice.common.models.Product;
import io.inprice.common.utils.URLUtils;

public class GenericImportService implements ImportService {

  private static final Logger log = LoggerFactory.getLogger(GenericImportService.class);

  private static final String ASIN_REGEX = "^(?i)(B0|BT)[0-9A-Z]{8}$";
  private static final String SKU_REGEX = "^[1-3][0-9]{10,11}$";

  public Response upload(ImportType importType, String content) {
    String identifier = null;
    final String[] regex = { "" };

    switch (importType) {
      case EBAY_SKU: {
        identifier = "SKU list";
        regex[0] = SKU_REGEX;
        break;
      }
      case AMAZON_ASIN: {
        identifier = "ASIN list";
        regex[0] = ASIN_REGEX;
        break;
      }
      case URL: {
        identifier = "URL list";
        regex[0] = URLUtils.URL_CHECK_REGEX;
        break;
      }
      case CSV: {
        return Responses.PermissionProblem.UNAUTHORIZED;
      }
    }

    Response[] res = { Responses.DataProblem.DB_PROBLEM };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transaction -> {
        CompanyDao companyDao = transaction.attach(CompanyDao.class);

        Company company = companyDao.findById(CurrentUser.getCompanyId());
        int allowedCount = company.getProductLimit() - company.getProductCount();

        Set<String> insertedSet = new HashSet<>();
        if (allowedCount > 0) {

          int actualCount = company.getProductCount();
          if (actualCount < allowedCount) {

            List<Map<String, String>> resultMapList = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
              LinkDao linkDao = transaction.attach(LinkDao.class);
              ProductDao productDao = transaction.attach(ProductDao.class);

              String line;
              while ((line = reader.readLine()) != null) {
                if (StringUtils.isBlank(line) || line.trim().startsWith("#")) {
                  continue;
                }

                String result = "";
                if (actualCount < allowedCount) {

                  boolean exists = insertedSet.contains(line);
                  if (! exists) {

                    if (line.matches(regex[0])) {
                      Response op = Responses.NotFound.PRODUCT;

                      if (ImportType.URL.equals(importType)) {
                        Link link = linkDao.findByUrlHashForImport(DigestUtils.md5Hex(line), CurrentUser.getCompanyId());
                        if (link != null) {
                          op = Responses.DataProblem.DUPLICATE;
                        }
                      } else {
                        Product product = productDao.findByCode(line, CurrentUser.getCompanyId());
                        if (product != null) {
                          op = Responses.DataProblem.DUPLICATE;
                        }
                      }

                      if (op.equals(Responses.NotFound.PRODUCT)) {
                        String url = null;

                        switch (importType) {
                          case EBAY_SKU: {
                            url = Props.PREFIX_FOR_SEARCH_EBAY() + line;
                            break;
                          }
                          case AMAZON_ASIN: {
                            url = Props.PREFIX_FOR_SEARCH_AMAZON() + line;
                            break;
                          }
                          default:
                            url = line;
                            break;
                        }

                        long id = linkDao.insert(url, DigestUtils.md5Hex(url), null, CurrentUser.getCompanyId());
                        if (id > 0) {
                          actualCount++;
                          insertedSet.add(line);
                          result = "Added successfully";
                        } else {
                          result = Responses.DataProblem.DB_PROBLEM.getReason();
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

              res[0] = new Response(resultMapList);
            }

          } else {
            res[0] = new Response("You have reached your plan's maximum product limit. To import more product, please select a broader plan.");
          }
        } else {
          res[0] = new Response("You haven't chosen a plan yet. You need to buy a plan to be able to import your products.");
        }

        return insertedSet.size() > 0;
      });
    } catch (Exception e) {
      log.error("An error occurred during importing " + identifier, e);
      res[0] = Responses.ServerProblem.EXCEPTION;
    }

    return res[0];
  }

}
