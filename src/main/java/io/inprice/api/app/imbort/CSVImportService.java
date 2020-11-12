package io.inprice.api.app.imbort;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import com.opencsv.CSVReader;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.app.product.ProductCreator;
import io.inprice.api.app.product.ProductDao;
import io.inprice.api.app.product.dto.ProductDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.meta.ImportType;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.ImportDetail;
import io.inprice.common.models.Product;
import io.inprice.common.utils.NumberUtils;

public class CSVImportService extends BaseImportService {

  private static final Logger log = LoggerFactory.getLogger(CSVImportService.class);

  private static final int COLUMN_COUNT = 3;

  @Override
  Response upload(String content) {
    Response[] res = { Responses.OK };
    
    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transactional -> {

        int successCount = 0;
        int problemCount = 0;
        long companyId = CurrentUser.getCompanyId();

        ImportDao importDao = transactional.attach(ImportDao.class);
        long importId = importDao.insert(ImportType.CSV.name(), companyId);
        if (importId == 0) {
          res[0] = Responses.DataProblem.DB_PROBLEM;
          return false;
        }

        CompanyDao companyDao = transactional.attach(CompanyDao.class);
        
        Company company = companyDao.findById(companyId);
        int allowedCount = company.getProductLimit() - company.getProductCount();

        Set<String> insertedSet = new HashSet<>();
        if (allowedCount > 0) {

          int actualCount = company.getProductCount();
          if (actualCount < allowedCount) {

            try (CSVReader csvReader = new CSVReader(new StringReader(content))) {
              ProductDao productDao = transactional.attach(ProductDao.class);
  
              String[] values;
              while ((values = csvReader.readNext()) != null) {
                if (values.length <= 1 || values[0].trim().isEmpty() || values[0].trim().equals("#")) {
                  continue;
                }
  
                String problem = null;
                LinkStatus status = LinkStatus.AVAILABLE;

                if (actualCount < allowedCount) {
                  boolean exists = insertedSet.contains(values[0]);
                  if (! exists) {

                    if (values.length == COLUMN_COUNT) {
                      Product product = productDao.findByCode(values[0], companyId);
                      if (product == null) {
  
                        ProductDTO dto = new ProductDTO();
                        dto.setCode(SqlHelper.clear(values[0]));
                        dto.setName(SqlHelper.clear(values[1]));
                        dto.setPrice(new BigDecimal(NumberUtils.extractPrice(values[2])));
                        dto.setCompanyId(companyId);

                        Response productCreateRes = ProductCreator.create(transactional, dto);
                        if (! productCreateRes.isOK()) {
                          status = LinkStatus.IMPROPER;
                          problem = productCreateRes.getReason();
                        }

                      } else {
                        status = LinkStatus.DUPLICATE;
                        problem = "Already defined!";
                      }
                    } else {
                      status = LinkStatus.IMPROPER;
                      problem = String.format("Column count must be %d, but is %d. Separator is comma ,", COLUMN_COUNT, values.length);
                    }
                  } else {
                    status = LinkStatus.IMPORTED;
                    problem = "Already imported!";
                  }
                } else {
                  status = LinkStatus.LIMIT_EXCEEDED;
                  problem = "Plan limit exceeded!";
                }

                insertedSet.add(values[0]);

                if (problem != null)
                  problemCount++;
                else
                  successCount++;

                ImportDetail impdet = new ImportDetail();
                impdet.setData(SqlHelper.clear(String.join(",", values)));
                impdet.setEligible(status.equals(LinkStatus.AVAILABLE));
                impdet.setImported(impdet.getEligible());
                impdet.setImportId(importId);
                impdet.setProblem(problem);
                impdet.setCompanyId(companyId);
                importDao.insertDetail(impdet);
              }
            }
          } else {
            res[0] = new Response("You have reached your plan limit. Please select a broader plan.");
          }
        } else {
          res[0] = new Response("Seems you haven't chosen a plan yet. Please consider buying a plan.");
        }

        boolean isOK = (insertedSet.size() > 0);
        if (isOK) {
          isOK = importDao.updateCounts(importId, successCount, problemCount);
          res[0] = new Response(importId);
        }

        return isOK;
      });
    } catch (Exception e) {
      log.error("An error occurred CSV list", e);
      res[0] = Responses.ServerProblem.EXCEPTION;
    }

    return res[0];

  }

}
