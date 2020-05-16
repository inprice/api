package io.inprice.scrapper.api.app.product_import;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.link.LinkStatus;
import io.inprice.scrapper.api.app.plan.PlanRepository;
import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;

public class ProductCodeImportService implements IProductImportService {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
  private static final ProductImportRepository productImportRepository = Beans.getSingleton(ProductImportRepository.class);
  private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

  private static final String ASIN_REGEX = "^(?i)(B0|BT)[0-9A-Z]{8}$";
  private static final String SKU_REGEX = "^[1-3][0-9]{11}$";

  public ServiceResponse upload(ImportType importType, String content) {
    ServiceResponse res = Responses.DataProblem.DB_PROBLEM;

    final String identifier = (ImportType.EBAY_SKU.equals(importType) ? "SKU" : "ASIN");
    final String regex = (ImportType.EBAY_SKU.equals(importType) ? SKU_REGEX : ASIN_REGEX);

    int allowedCount = planRepository.findAllowedProductCount();
    if (allowedCount > 0) {

      int actualCount = productRepository.findProductCount();
      if (actualCount < allowedCount) {

        Set<String> insertedCodeSet = new HashSet<>();
        List<ImportProduct> importList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
          String line = reader.readLine().trim();
          while (line != null) {
            if (StringUtils.isBlank(line) || line.startsWith("#")) continue;

            ImportProduct row = new ImportProduct();
            row.setImportType(importType);
            row.setData(line);

            if (actualCount < allowedCount) {
              boolean found = insertedCodeSet.contains(line);
              if (!found) {
                if (line.matches(regex)) {
                  row.setDescription("Healthy.");
                  insertedCodeSet.add(line);
                  actualCount++;
                } else {
                  row.setStatus(LinkStatus.IMPROPER);
                }
              } else {
                row.setStatus(LinkStatus.DUPLICATE);
              }
            } else {
              row.setStatus(LinkStatus.WONT_BE_IMPLEMENTED);
            }
            importList.add(row);
            line = reader.readLine();
          }
        } catch (Exception e) {
          log.error("Failed to import " + identifier + " list.", e);
          return Responses.DataProblem.DB_PROBLEM;
        }

        if (actualCount > 0) {
          res = productImportRepository.bulkInsert(importList);
        } else {
          res = new ServiceResponse("Failed to import " + identifier + " list, please check your data!");
        }

      } else {
        res = new ServiceResponse("You have reached your plan's maximum product limit.");
      }

    } else {
      res = new ServiceResponse("You haven't chosen a plan yet. You need to buy a plan to be able to import your products.");
    }

    return res;
  }

}
