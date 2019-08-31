package io.inprice.scrapper.api.rest.service;

import com.opencsv.CSVReader;
import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
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
import org.eclipse.jetty.http.HttpStatus;
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

    private final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);
    private final PlanRepository planRepository = Beans.getSingleton(PlanRepository.class);

    private static final int COLUMN_COUNT = 5;

    public ImportProduct upload(String file) {
        ImportProduct imbort = new ImportProduct();
        imbort.setImportType(ImportType.CSV);
        imbort.setStatus(HttpStatus.BAD_REQUEST_400);
        imbort.setProblemList(new ArrayList<>());

        int actualProdCount = productRepository.findProductCount();
        int allowedProdCount = planRepository.findAllowedProductCount();

        if (actualProdCount > -1 && allowedProdCount > -1) {
            if (actualProdCount < allowedProdCount) {

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

                                ServiceResponse<Product> validation = ProductDTOValidator.validate(dto);
                                if (validation.isOK()) {
                                    importRow.setProductDTO(dto);
                                    importRow.setDescription("Healthy.");
                                    importRow.setStatus(Status.AVAILABLE);
                                    actualProdCount++;
                                    imbort.incInsertCount();
                                } else {
                                    StringBuilder sb = new StringBuilder();
                                    for (Problem problem: validation.getProblems()) {
                                        if (sb.length() != 0) sb.append(" | ");
                                        sb.append(problem.getReason());
                                    }
                                    importRow.setDescription(sb.toString());
                                    importRow.setStatus(Status.IMPROPER);
                                    imbort.incProblemCount();
                                }

                            } else {
                                importRow.setDescription("There must be " + COLUMN_COUNT + " columns in each row!. Column separator is comma ,");
                                importRow.setStatus(Status.IMPROPER);
                                imbort.incProblemCount();
                            }
                        } else {
                            importRow.setDescription("You have reached your plan's maximum product limit.");
                            importRow.setStatus(Status.WONT_BE_IMPLEMENTED);
                            imbort.incProblemCount();
                        }
                        importList.add(importRow);

                        imbort.incTotalCount();

                        if (! Status.AVAILABLE.equals(importRow.getStatus())) {
                            imbort.getProblemList().add("Row: " + imbort.getTotalCount() + ". " + importRow.getDescription());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to import a csv file.", e);
                    imbort.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    imbort.setResult("Server error: " + e.getMessage());
                }

                if (imbort.getInsertCount() > 0) {
                    imbort.setStatus(HttpStatus.OK_200);
                    if (imbort.getProblemCount() == 0) {
                        imbort.setResult("CSV file has been successfully uploaded.");
                    } else {
                        imbort.setResult("CSV file has been uploaded. However, some problems occurred. Please see details.");
                    }
                } else {
                    imbort.setStatus(HttpStatus.BAD_REQUEST_400);
                    imbort.setResult("Failed to import CSV file, please see details!");
                }

                ServiceResponse bulkResponse = productRepository.bulkInsert(imbort, importList);
                if (! bulkResponse.isOK()) {
                    imbort.setStatus(bulkResponse.getStatus());
                    imbort.setResult(bulkResponse.getResult());
                }

            } else {
                imbort.setStatus(HttpStatus.TOO_MANY_REQUESTS_429);
                imbort.setResult("You have already reached your plan's maximum product limit.");
            }
        } else {
            imbort.setStatus(HttpStatus.EXPECTATION_FAILED_417);
            imbort.setResult("Seems you haven't chosen a plan yet. You need to select one to import your products.");
        }

        if (imbort.getProblemList().size() == 0) imbort.setProblemList(null);

        return imbort;
    }

}
