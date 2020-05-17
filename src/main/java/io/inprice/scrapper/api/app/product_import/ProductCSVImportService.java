package io.inprice.scrapper.api.app.product_import;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.link.LinkStatus;
import io.inprice.scrapper.api.app.plan.PlanRepository;
import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.dto.ProductDTOValidator;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.utils.NumberUtils;

public class ProductCSVImportService implements IProductImportService {

  private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
  private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
  private static final ProductImportRepository productImportRepository = Beans.getSingleton(ProductImportRepository.class);
  private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

  private static final int COLUMN_COUNT = 5;

  public ServiceResponse upload(String content) {
    ServiceResponse res = Responses.OK;

    int allowedCount = planRepository.findAllowedProductCount();
    if (allowedCount > 0) {

      int actualCount = productRepository.findProductCount();
      if (actualCount < allowedCount) {

        Map<String, ProductDTO> insertedProductSet = new HashMap<>();
        List<ImportProduct> importList = new ArrayList<>();

        try (CSVReader csvReader = new CSVReader(new StringReader(content))) {
          String[] values;
          while ((values = csvReader.readNext()) != null) {
            if (values.length == 1 || values[0].trim().isEmpty() || values[0].trim().equals("#")) continue;

            ImportProduct row = new ImportProduct();
            row.setImportType(ImportType.CSV);
            row.setData(StringUtils.join(values, ","));

            if (actualCount < allowedCount) {

              if (values.length == COLUMN_COUNT) {
                boolean found = insertedProductSet.containsKey(values[0]);

                if (!found) {
                  int i = 0;
                  ProductDTO dto = new ProductDTO();
                  dto.setCode(values[i++]);
                  dto.setName(values[i++]);
                  dto.setBrand(values[i++]);
                  dto.setCategory(values[i++]);
                  dto.setPrice(new BigDecimal(NumberUtils.extractPrice(values[i])));
  
                  ServiceResponse isValid = ProductDTOValidator.validate(dto);
                  if (isValid.isOK()) {
                    row.setDescription("Added among the products.");
                    row.setStatus(LinkStatus.NEW);
                    row.setProductDTO(dto);
                    actualCount++;
                    insertedProductSet.put(dto.getCode(), dto);
                  } else {
                    StringBuilder sb = new StringBuilder();
                    for (String problem : isValid.getProblems()) {
                      if (sb.length() != 0)
                        sb.append(" & ");
                      sb.append(problem);
                    }
                    row.setDescription(sb.toString());
                    row.setStatus(LinkStatus.IMPROPER);
                  }
                } else {
                  row.setStatus(LinkStatus.DUPLICATE);
                }
              } else {
                row.setDescription(String.format(
                    "Expected col count is %d, but %d. Separator is comma ,",
                    COLUMN_COUNT, values.length));
                row.setStatus(LinkStatus.IMPROPER);
              }

            } else {
              row.setDescription("You have reached your plan's maximum product limit.");
              row.setStatus(LinkStatus.WONT_BE_IMPLEMENTED);
            }
            importList.add(row);
          }
        } catch (Exception e) {
          log.error("Failed to import csv file.", e);
          return Responses.DataProblem.DB_PROBLEM;
        }

        if (insertedProductSet.size() > 0) {
          res = productImportRepository.bulkInsert(importList);
        } else {
          res = new ServiceResponse("Failed to import CSV file, please check your data!");
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
