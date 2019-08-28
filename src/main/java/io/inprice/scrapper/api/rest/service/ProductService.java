package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ImportProblem;
import io.inprice.scrapper.api.info.ImportReport;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.api.rest.validator.ProductDTOValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);

    private final ProductRepository repository = Beans.getSingleton(ProductRepository.class);

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse getList() {
        return repository.getList();
    }

    public ServiceResponse insert(ProductDTO productDTO) {
        ServiceResponse res = ProductDTOValidator.validate(productDTO);
        if (res.isOK()) {
            res = repository.insert(productDTO);
        }
        return res;
    }

    public ServiceResponse update(ProductDTO productDTO) {
        if (productDTO.getId() == null || productDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        ServiceResponse res = ProductDTOValidator.validate(productDTO);
        if (res.isOK()) {
            res = repository.update(productDTO);
        }
        return res;
    }

    public ServiceResponse deleteById(Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        return repository.deleteById(id);
    }

    public ServiceResponse toggleStatus(Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        return repository.toggleStatus(id);
    }

    public ImportReport uploadAmazonASIN(String file) {
        ImportReport report = new ImportReport(HttpStatus.OK_200);
        report.setProblems(new ArrayList<>());

        List<ProductDTO> prodList = new ArrayList<>();
        try {
            int row = 1;
            int prodCount = repository.findProductCount();

            Scanner scanner = new Scanner(file);

            while (scanner.hasNext()) {
                String asin = scanner.nextLine();

                if (asin.matches("^(?i)(B0|BT)[0-9A-Z]{8}$")) {
                    System.out.println("https://www.amazon.com/dp/" + asin);
                } else {
                    ImportProblem ip = new ImportProblem("Line: " + row + ". Code: " + asin);
                    report.getProblems().add(ip);
                }

                //todo: must be limited with workspace's plan limit
                if (prodCount > 100) {
                    break;
                }

                row++;
            }
            scanner.close();

            if (report.getProblems() == null || report.getProblems().size() == 0) {
                //todo: some logic must be added into repository in order to save links to become product
                ServiceResponse res = repository.bulkInsert(report, prodList);
                if (res.isOK()) {
                    report.setResult("Amazon ASIN list file has been successfully uploaded.");
                } else {
                    report.setStatus(HttpStatus.BAD_REQUEST_400);
                    report.setResult(res.getResult());
                }
            } else {
                report.setStatus(HttpStatus.BAD_REQUEST_400);
                report.setResult("Failed to upload Amazon ASIN list file, please see details.");
            }
        } catch (Exception e) {
            log.error("Failed to import Amazon ASIN list file.", e);
            report = new ImportReport(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
        }
        return report;
    }

}
