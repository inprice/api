package io.inprice.scrapper.api.rest.service;

import com.opencsv.CSVReader;
import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.PlanRepository;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.api.rest.validator.ProductDTOValidator;
import io.inprice.scrapper.common.meta.ImportType;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.ImportProduct;
import io.inprice.scrapper.common.models.ImportProductRow;
import io.inprice.scrapper.common.models.Product;
import io.inprice.scrapper.common.utils.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductCSVImportService {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
    private static final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
    private static final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

    private static final int COLUMN_COUNT = 5;

    public ImportProduct upload(String file) {
        ImportProduct report = new ImportProduct();
        report.setImportType(ImportType.CSV);

        int allowedProdCount = planRepository.findAllowedProductCount();
        if (allowedProdCount > 0) {
            report.setProblemList(new ArrayList<>());

            int actualProdCount = productRepository.findProductCount();
            if (actualProdCount < allowedProdCount) {

                Set<String> insertedCodeSet = new HashSet<>();

                List<ImportProductRow> importList = new ArrayList<>();
                try (CSVReader csvReader = new CSVReader(new StringReader(file))) {

                    String[] values;
                    while ((values = csvReader.readNext()) != null) {
                        ImportProductRow importRow = new ImportProductRow();
                        importRow.setImportType(ImportType.CSV);
                        importRow.setStatus(Status.NEW);
                        importRow.setData(String.join(",", values));

                        if (actualProdCount < allowedProdCount) {

                            if (values.length == COLUMN_COUNT) {
                                int i = 0;
                                ProductDTO dto = new ProductDTO();
                                dto.setCode(values[i++]);
                                dto.setName(values[i++]);
                                dto.setBrand(values[i++]);
                                dto.setCategory(values[i++]);
                                dto.setPrice(new BigDecimal(NumberUtils.extractPrice(values[i])));

                                boolean found = insertedCodeSet.contains(dto.getCode());
                                if (found) {
                                    importRow.setDescription("Already exists!");
                                    importRow.setStatus(Status.DUPLICATE);
                                    report.incDuplicateCount();
                                } else {
                                    ServiceResponse<Product> validation = ProductDTOValidator.validate(dto);
                                    if (validation.isOK()) {
                                        importRow.setProductDTO(dto);
                                        importRow.setDescription("Healthy.");
                                        importRow.setStatus(Status.AVAILABLE);
                                        actualProdCount++;
                                        report.incInsertCount();
                                        insertedCodeSet.add(dto.getCode());
                                    } else {
                                        StringBuilder sb = new StringBuilder();
                                        for (Problem problem : validation.getProblems()) {
                                            if (sb.length() != 0) sb.append(" & ");
                                            sb.append(problem.getReason());
                                        }
                                        importRow.setDescription(sb.toString());
                                        importRow.setStatus(Status.IMPROPER);
                                        report.incProblemCount();
                                    }
                                }

                            } else {
                                importRow.setDescription("There must be " + COLUMN_COUNT + " columns in each row!. Column separator is comma ,");
                                importRow.setStatus(Status.IMPROPER);
                                report.incProblemCount();
                            }
                        } else {
                            importRow.setDescription("You have reached your plan's maximum product limit.");
                            importRow.setStatus(Status.WONT_BE_IMPLEMENTED);
                            report.incProblemCount();
                        }
                        importList.add(importRow);

                        report.incTotalCount();

                        if (!Status.AVAILABLE.equals(importRow.getStatus())) {
                            report.getProblemList().add(StringUtils.leftPad(""+report.getTotalCount(), 3, '0') + ": " + importRow.getDescription());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to import a csv file.", e);
                    report.setStatus(Responses.ServerProblem.EXCEPTION.getStatus());
                    report.setResult("Server error: " + e.getMessage());
                }

                if (report.getInsertCount() > 0) {
                    report.setStatus(Responses.OK.getStatus());
                    if (report.getProblemCount() == 0) {
                        report.setResult("CSV file has been successfully uploaded.");
                    } else {
                        report.setResult("CSV file has been uploaded. However, some problems occurred. Please see details.");
                    }
                } else {
                    report.setStatus(Responses.DataProblem.NOT_SUITABLE.getStatus());
                    report.setResult("Failed to import CSV file, please see details!");
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
