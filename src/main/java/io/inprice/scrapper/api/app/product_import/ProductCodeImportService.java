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

import io.inprice.scrapper.api.app.plan.PlanRepository;
import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.meta.ImportType;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.ImportProduct;

public class ProductCodeImportService implements IProductImportService {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
  private static final ProductImportRepository productImportRepository = Beans.getSingleton(ProductImportRepository.class);
  private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

  private static final String ASIN_REGEX = "^(?i)(B0|BT)[0-9A-Z]{8}$";
  private static final String SKU_REGEX = "^[1-3][0-9]{10,11}$";

  public ServiceResponse upload(ImportType importType, String content) {
    ServiceResponse res = Responses.DataProblem.DB_PROBLEM;

    String company = "Ebay";
    String identifier = "SKU";
    String regex = SKU_REGEX;

    if (ImportType.AMAZON_ASIN.equals(importType)) {
      company = "Amazon";
      identifier = "ASIN";
      regex = ASIN_REGEX;
    }

    int allowedCount = planRepository.findAllowedProductCount();
    if (allowedCount > 0) {

      int actualCount = productRepository.findProductCount();
      if (actualCount < allowedCount) {

        Set<String> insertedCodeSet = new HashSet<>();
        List<ImportProduct> importList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
          String line = reader.readLine().trim();
          while (line != null) {
            if (StringUtils.isBlank(line) || line.trim().startsWith("#")) {
              line = reader.readLine();
              continue;
            }

            ImportProduct row = new ImportProduct();
            row.setImportType(importType);

            switch (importType) {
              case EBAY_SKU: {
                row.setData(Props.PREFIX_FOR_SEARCH_EBAY() + line);
                break;
              }
              case AMAZON_ASIN: {
                row.setData(Props.PREFIX_FOR_SEARCH_AMAZON() + line);
                break;
              }
              default:
                row.setData(line);
                break;
            }
    
            if (actualCount < allowedCount) {
              boolean exists = insertedCodeSet.contains(line);
              if (! exists) {
                if (line.matches(regex)) {

                  ServiceResponse found = productRepository.findByCode(line);
                  if (! found.isOK()) {
                    found = productImportRepository.findByData(row.getData());
                  }

                  if (! found.isOK()) {
                    row.setDescription("This prod. will be updated shortly via " + company);
                    insertedCodeSet.add(line);
                    actualCount++;
                  } else {
                    row.setStatus(LinkStatus.DUPLICATE);
                  }

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

        if (insertedCodeSet.size() > 0) {
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
