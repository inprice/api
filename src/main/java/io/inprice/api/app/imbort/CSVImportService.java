package io.inprice.api.app.imbort;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opencsv.CSVReader;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.app.lookup.LookupDao;
import io.inprice.api.app.product.ProductCreator;
import io.inprice.api.app.product.ProductDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.ProductDTO;
import io.inprice.common.meta.LookupType;
import io.inprice.common.models.Company;
import io.inprice.common.models.Lookup;
import io.inprice.common.models.Product;
import io.inprice.common.utils.NumberUtils;

public class CSVImportService implements ImportService {

  private static final Logger log = LoggerFactory.getLogger(CSVImportService.class);

  private static final int COLUMN_COUNT = 5;

  public Response upload(String content) {
    Response[] res = { Responses.DataProblem.DB_PROBLEM };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(h -> {
        CompanyDao companyDao = h.attach(CompanyDao.class);

        Company company = companyDao.findById(CurrentUser.getCompanyId());
        int allowedCount = company.getProductLimit() - company.getProductCount();

        Set<String> insertedSet = new HashSet<>();
        if (allowedCount > 0) {

          int actualCount = company.getProductCount();
          if (actualCount < allowedCount) {

            List<Map<String, String>> resultMapList = new ArrayList<>();
  
            try (CSVReader csvReader = new CSVReader(new StringReader(content))) {
              LookupDao lookupDao = h.attach(LookupDao.class);
              ProductDao productDao = h.attach(ProductDao.class);
  
              Map<String, Lookup> brandsMap = lookupDao.findMapByType(LookupType.BRAND.name(), CurrentUser.getCompanyId());
              Map<String, Lookup> categoriesMap = lookupDao.findMapByType(LookupType.CATEGORY.name(), CurrentUser.getCompanyId());
  
              String[] values;
              while ((values = csvReader.readNext()) != null) {
                if (values.length <= 1 || values[0].trim().isEmpty() || values[0].trim().equals("#")) continue;
  
                String result = "";
  
                if (actualCount < allowedCount) {
                  boolean exists = insertedSet.contains(values[0]);
                  if (! exists) {
                    if (values.length == COLUMN_COUNT) {
  
                      Product product = productDao.findByCode(values[0], CurrentUser.getCompanyId());
                      if (product == null) {
  
                        ProductDTO dto = new ProductDTO();
                        dto.setCode(SqlHelper.clear(values[0]));
                        dto.setName(SqlHelper.clear(values[1]));
                        dto.setPrice(new BigDecimal(NumberUtils.extractPrice(values[4])));
                        dto.setCompanyId(CurrentUser.getCompanyId());

                        String brandName = SqlHelper.clear(values[2]);
                        String categoryName = SqlHelper.clear(values[3]);
                        
                        if (StringUtils.isNotBlank(brandName)) {
                          Lookup brand = brandsMap.get(brandName);
                          if (brand == null) {
                            brand = lookupDao.insert(LookupType.BRAND.name(), brandName, CurrentUser.getCompanyId());
                            brandsMap.put(brandName, brand);
                          }
                          dto.setBrandId(brand.getId());
                        }
  
                        if (StringUtils.isNotBlank(categoryName)) {
                          Lookup category = categoriesMap.get(categoryName);
                          if (category == null) {
                            category = lookupDao.insert(LookupType.CATEGORY.name(), categoryName, CurrentUser.getCompanyId());
                            categoriesMap.put(categoryName, category);
                          }
                          dto.setCategoryId(category.getId());
                        }
                        
                        Response productCreateRes = ProductCreator.create(h, dto);
                        if (productCreateRes.isOK()) {
                          actualCount++;
                          insertedSet.add(values[0]);
                          result = "Added successfully";
                        } else {
                          result = productCreateRes.getReason();
                        }

                      } else {
                        result = String.format("The code {} is already defined!", values[0]);
                      }
                    } else {
                      result = String.format("Expected col count is %d, but %d. Separator is comma ,", COLUMN_COUNT, values.length);
                    }
                  } else {
                    result = "This is already handled. Please see previous rows in the list!";
                  }
                } else {
                  result = "Your product count reached the limit of your plan. Allowed prod. count: " + allowedCount;
                }
  
                Map<String, String> resultMap = new HashMap<>(2);
                resultMap.put("line", values[0] + " : " + values[1]);
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
      log.error("An error occurred CSV list", e);
      res[0] = Responses.ServerProblem.EXCEPTION;
    }

    return res[0];
  }

}
