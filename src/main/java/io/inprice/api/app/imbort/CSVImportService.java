package io.inprice.api.app.imbort;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.opencsv.CSVReader;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
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
import io.inprice.common.models.Account;
import io.inprice.common.models.ImportDetail;
import io.inprice.common.models.Product;
import io.inprice.common.utils.NumberUtils;

public class CSVImportService extends BaseImportService {

  private static final Logger log = LoggerFactory.getLogger(CSVImportService.class);

  private static final int COLUMN_COUNT = 3;

  @Override
  Response upload(String content, boolean isFile) {
    Response[] res = { Responses.OK };
    
    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transactional -> {

        int successCount = 0;
        int problemCount = 0;
        long accountId = CurrentUser.getAccountId();

        ImportDao importDao = transactional.attach(ImportDao.class);
        long importId = importDao.insert(ImportType.CSV.name(), isFile, accountId);
        if (importId == 0) {
          res[0] = Responses.DataProblem.DB_PROBLEM;
          return false;
        }

        AccountDao accountDao = transactional.attach(AccountDao.class);
        
        Account account = accountDao.findById(accountId);
        int allowedCount = account.getProductLimit() - account.getProductCount();

        Set<String> insertedSet = new HashSet<>();
        if (allowedCount > 0) {

          int actualCount = account.getProductCount();
          if (actualCount < allowedCount) {

            try (CSVReader csvReader = new CSVReader(new StringReader(content))) {
              ProductDao productDao = transactional.attach(ProductDao.class);
  
              String[] values;
              while ((values = csvReader.readNext()) != null) {
                if (values.length != 3 || values[0].trim().isEmpty() || values[0].trim().charAt(0) == '#') {
                  continue;
                }
  
                String problem = null;
                LinkStatus status = LinkStatus.AVAILABLE;

                if (actualCount < allowedCount) {
                  boolean exists = insertedSet.contains(values[0]);
                  if (! exists) {

                    if (values.length == COLUMN_COUNT) {
                      Product product = productDao.findByCode(values[0], accountId);
                      if (product == null) {
  
                        ProductDTO dto = new ProductDTO();
                        dto.setCode(SqlHelper.clear(values[0]));
                        dto.setName(SqlHelper.clear(values[1]));
                        dto.setPrice(new BigDecimal(NumberUtils.extractPrice(values[2])));
                        dto.setAccountId(accountId);

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
                impdet.setStatus(problem != null ? problem : "IMPORTED");
                impdet.setAccountId(accountId);
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
          Map<String, Object> data = new HashMap<>(2);
          data.put("importId", importId);
          data.put("successes", successCount);
          res[0] = new Response(data);
        } else {
          importDao.delete(importId);
          //res[0] = Responses.Illegal.INCOMPATIBLE_CONTENT;
        }

        return isOK;
      });
    } catch (Exception e) {
      log.error("Failed to upload CSV list", e);
      res[0] = Responses.ServerProblem.EXCEPTION;
    }

    return res[0];

  }

}
