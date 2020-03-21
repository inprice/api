package io.inprice.scrapper.api.app.product_import;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.link.LinkStatus;
import io.inprice.scrapper.api.app.plan.PlanRepository;
import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.utils.URLUtils;

public class ProductURLImportService implements IProductImportService {

   private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
   private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
   private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

   public ImportProduct upload(String content) {
      ImportProduct report = new ImportProduct();
      report.setImportType(ImportType.URL);

      int allowedProdCount = planRepository.findAllowedProductCount();
      if (allowedProdCount > 0) {
         report.setProblemList(new ArrayList<>());

         int actualProdCount = productRepository.findProductCount();
         if (actualProdCount < allowedProdCount) {

            Set<String> insertedURLSet = new HashSet<>();

            List<ImportProductRow> importList = new ArrayList<>();
            try (Scanner scanner = new Scanner(file)) {

               int row = 1;
               while (scanner.hasNext()) {
                  String line = scanner.nextLine();

                  if (!StringUtils.isBlank(line) && !line.startsWith("#")) {

                     ImportProductRow importRow = new ImportProductRow();
                     importRow.setImportType(report.getImportType());
                     importRow.setStatus(LinkStatus.NEW);
                     importRow.setData(line);

                     if (actualProdCount < allowedProdCount) {
                        boolean found = insertedURLSet.contains(line);
                        if (found) {
                           importRow.setDescription("Already exists!");
                           importRow.setStatus(LinkStatus.DUPLICATE);
                           report.incDuplicateCount();
                        } else {
                           boolean isValid = URLUtils.isAValidURL(line);
                           if (isValid) {
                              importRow.setDescription("Healthy.");
                              importRow.setStatus(LinkStatus.NEW);
                              actualProdCount++;
                              report.incInsertCount();
                              insertedURLSet.add(line);
                           } else {
                              importRow.setDescription("Invalid URL!");
                              importRow.setStatus(LinkStatus.IMPROPER);
                              report.incProblemCount();
                           }
                        }
                     } else {
                        importRow.setDescription("You have reached your plan's maximum product limit.");
                        importRow.setStatus(LinkStatus.WONT_BE_IMPLEMENTED);
                        report.incProblemCount();
                     }
                     importList.add(importRow);

                     report.incTotalCount();

                     if (!LinkStatus.NEW.equals(importRow.getStatus())) {
                        report.getProblemList().add(row + ". " + importRow.getDescription());
                     }
                  }

                  row++;
               }
            } catch (Exception e) {
               log.error("Failed to import URL list.", e);
               report.setStatus(Responses.ServerProblem.EXCEPTION.getStatus());
               report.setResult("Server error: " + e.getMessage());
            }

            if (report.getInsertCount() > 0) {
               report.setStatus(Responses.OK.getStatus());
               if (report.getProblemCount() == 0) {
                  report.setResult("URL list has been successfully uploaded.");
               } else {
                  report.setResult("URL list has been uploaded. However, some problems occurred. Please see details.");
               }
            } else {
               report.setStatus(Responses.DataProblem.NOT_SUITABLE.getStatus());
               report.setResult("Failed to import URL list, please see details!");
            }

            ServiceResponse bulkResponse = productRepository.bulkInsert(report, importList);
            if (!bulkResponse.isOK()) {
               report.setStatus(bulkResponse.getStatus());
            }

         } else {
            report.setStatus(Responses.ServerProblem.LIMIT_PROBLEM.getStatus());
            report.setResult("You have already reached your plan's maximum product limit.");
         }

         if (report.getProblemList().size() == 0)
            report.setProblemList(null);

      } else {
         report.setStatus(Responses.PermissionProblem.DONT_HAVE_A_PLAN.getStatus());
         report.setResult(
               "Seems you haven't chosen a plan yet. You need to buy a plan to be able to import your products.");
      }

      return report;
   }

}
