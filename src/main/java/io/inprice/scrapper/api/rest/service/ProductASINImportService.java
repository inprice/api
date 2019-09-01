package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.PlanRepository;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.common.meta.ImportType;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.ImportProduct;
import io.inprice.scrapper.common.models.ImportProductRow;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProductASINImportService {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);

    private final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
    private final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

    private static final String REGEX = "^(?i)(B0|BT)[0-9A-Z]{8}$";

    public ImportProduct upload(String file) {
        ImportProduct report = new ImportProduct();
        report.setImportType(ImportType.AMAZON_ASIN);
        report.setStatus(HttpStatus.BAD_REQUEST_400);

        int allowedProdCount = planRepository.findAllowedProductCount();
        if (allowedProdCount > 0) {
            report.setProblemList(new ArrayList<>());

            int actualProdCount = productRepository.findProductCount();
            if (actualProdCount < allowedProdCount) {

                Set<String> insertedCodeSet = new HashSet<>();

                List<ImportProductRow> importList = new ArrayList<>();
                try (Scanner scanner = new Scanner(file)) {
                    while (scanner.hasNext()) {
                        String line = scanner.nextLine();

                        if (line.trim().isEmpty() || line.startsWith("#")) continue;

                        ImportProductRow importRow = new ImportProductRow();
                        importRow.setImportType(ImportType.AMAZON_ASIN);
                        importRow.setStatus(Status.NEW);
                        importRow.setData(line);

                        if (actualProdCount < allowedProdCount) {
                            boolean found = insertedCodeSet.contains(line);
                            if (found) {
                                importRow.setDescription("Already exists!");
                                importRow.setStatus(Status.DUPLICATE);
                                report.incDuplicateCount();
                            } else {
                                boolean isValid = line.matches(REGEX);
                                if (isValid) {
                                    importRow.setDescription("Healthy.");
                                    importRow.setStatus(Status.NEW);
                                    actualProdCount++;
                                    report.incInsertCount();
                                    insertedCodeSet.add(line);
                                } else {
                                    importRow.setDescription("Invalid ASIN code!");
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
                            report.getProblemList().add(StringUtils.leftPad(""+report.getTotalCount(), 3, '0') + ": " + importRow.getDescription());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to import ASIN list.", e);
                    report.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    report.setResult("Server error: " + e.getMessage());
                }

                if (report.getInsertCount() > 0) {
                    report.setStatus(HttpStatus.OK_200);
                    if (report.getProblemCount() == 0) {
                        report.setResult("ASIN list has been successfully uploaded.");
                    } else {
                        report.setResult("ASIN list has been uploaded. However, some problems occurred. Please see details.");
                    }
                } else {
                    report.setStatus(HttpStatus.BAD_REQUEST_400);
                    report.setResult("Failed to import ASIN list, please see details!");
                }

                ServiceResponse bulkResponse = productRepository.bulkInsert(report, importList);
                if (!bulkResponse.isOK()) {
                    report.setStatus(bulkResponse.getStatus());
                    report.setResult(bulkResponse.getResult());
                }

            } else {
                report.setStatus(HttpStatus.TOO_MANY_REQUESTS_429);
                report.setResult("You have already reached your plan's maximum product limit.");
            }

            if (report.getProblemList().size() == 0) report.setProblemList(null);

        } else {
            report.setStatus(HttpStatus.EXPECTATION_FAILED_417);
            report.setResult("Seems you haven't chosen a plan yet. You need to buy a plan to be able to import your products.");
        }

        return report;
    }

}
