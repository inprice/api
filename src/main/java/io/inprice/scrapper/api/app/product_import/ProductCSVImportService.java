package io.inprice.scrapper.api.app.product_import;

import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opencsv.CSVReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.company.CompanyRepository;
import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.ProductDTOValidator;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.helpers.Database;
import io.inprice.scrapper.common.info.ProductDTO;
import io.inprice.scrapper.common.models.Company;
import io.inprice.scrapper.common.utils.NumberUtils;

public class ProductCSVImportService implements IProductImportService {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);
  private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
  private static final Database db = Beans.getSingleton(Database.class);

  private static final int COLUMN_COUNT = 5;

  public ServiceResponse upload(String content) {
    ServiceResponse res = Responses.DataProblem.DB_PROBLEM;

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

          try (CSVReader csvReader = new CSVReader(new StringReader(content))) {

            String[] values;
            while ((values = csvReader.readNext()) != null) {
              if (values.length <= 1 || values[0].trim().isEmpty() || values[0].trim().equals("#")) continue;

              String result = "";

              if (actualCount < allowedCount) {
                boolean exists = insertedCodeSet.contains(values[0]);
                if (! exists) {
                  if (values.length == COLUMN_COUNT) {

                    ServiceResponse op = productRepository.findByCode(con, values[0]);
                    if (! op.isOK()) {

                      int i = 0;
                      ProductDTO dto = new ProductDTO();
                      dto.setCode(values[i++]);
                      dto.setName(values[i++]);
                      dto.setBrand(values[i++]);
                      dto.setCategory(values[i++]);
                      dto.setPrice(new BigDecimal(NumberUtils.extractPrice(values[i])));
                      dto.setCompanyId(CurrentUser.getCompanyId());
      
                      ServiceResponse isValid = ProductDTOValidator.validate(dto);
                      if (isValid.isOK()) {
                        op = productRepository.insertANewProduct(con, dto);
                        if (op.isOK()) {
                          actualCount++;
                          insertedCodeSet.add(values[0]);
                          result = "Added successfully";
                        } else {
                          result = op.getReason();
                        }
                      } else {
                        StringBuilder sb = new StringBuilder();
                        for (String problem : isValid.getProblems()) {
                          if (sb.length() != 0) sb.append(" & ");
                          sb.append(problem);
                        }
                        result = sb.toString();
                      }
                    } else {
                      result = "The code " + values[0] + " is already defined!";
                    }
                  } else {
                    result = String.format(
                      "Expected col count is %d, but %d. Separator is comma ,", COLUMN_COUNT, values.length);
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
      log.error("Failed to import CSV file.", e);
      res = Responses.DataProblem.DB_PROBLEM;
    } finally {
      if (con != null) {
        db.close(con);
      }
    }

    return res;
  }

}
