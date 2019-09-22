package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.PlanRepository;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.common.meta.ImportType;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.ImportProduct;
import io.inprice.scrapper.common.models.ImportProductRow;
import io.inprice.scrapper.common.utils.URLUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProductURLImportService {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
    private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
    private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

    public ImportProduct upload(String file) {
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

                        if (! StringUtils.isBlank(line) && ! line.startsWith("#")) {

                            ImportProductRow importRow = new ImportProductRow();
                            importRow.setImportType(report.getImportType());
                            importRow.setStatus(Status.NEW);
                            importRow.setData(line);

                            if (actualProdCount < allowedProdCount) {
                                boolean found = insertedURLSet.contains(line);
                                if (found) {
                                    importRow.setDescription("Already exists!");
                                    importRow.setStatus(Status.DUPLICATE);
                                    report.incDuplicateCount();
                                } else {
                                    boolean isValid = URLUtils.isAValidURL(line);
                                    if (isValid) {
                                        importRow.setDescription("Healthy.");
                                        importRow.setStatus(Status.NEW);
                                        actualProdCount++;
                                        report.incInsertCount();
                                        insertedURLSet.add(line);
                                    } else {
                                        importRow.setDescription("Invalid URL !");
                                        importRow.setStatus(Status.IMPROPER);
                                        report.incProblemCount();
                                    }
                                }
                            } else {
                                importRow.setDescription("You have reached your plan's maximum product limit.");
                                importRow.setStatus(Status.WONT_BE_IMPLEMENTED);
                                report.incProblemCount();
                            }
                            importList.add(importRow);

                            report.incTotalCount();

                            if (!Status.NEW.equals(importRow.getStatus())) {
                                report.getProblemList().add(StringUtils.leftPad(""+row, 3, '0') + ": " + importRow.getDescription());
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

            if (report.getProblemList().size() == 0) report.setProblemList(null);

        } else {
            report.setStatus(Responses.PermissionProblem.DONT_HAVE_A_PLAN.getStatus());
            report.setResult("Seems you haven't chosen a plan yet. You need to buy a plan to be able to import your products.");
        }

        return report;
    }

}
