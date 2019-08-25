package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.*;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.common.utils.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
    private final ProductRepository repository = Beans.getSingleton(ProductRepository.class);

    private static final char DEFAULT_SEPARATOR = ';';
    private static final char DEFAULT_QUOTE = '"';

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse getList() {
        return repository.getList();
    }

    public ServiceResponse insert(ProductDTO productDTO) {
        ServiceResponse res = validate(productDTO);
        if (res.isOK()) {
            res = repository.insert(productDTO);
        }
        return res;
    }

    public ServiceResponse update(ProductDTO productDTO) {
        if (productDTO.getId() == null || productDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        ServiceResponse res = validate(productDTO);
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

    public ImportReport uploadCSV(String file) {
        ImportReport report = new ImportReport(HttpStatus.OK_200);
        report.setProblems(new ArrayList<>());

        List<ProductDTO> prodList = new ArrayList<>();
        try {
            int row = 1;
            int prodCount = repository.findProductCount();

            boolean formatError = false;
            Scanner scanner = new Scanner(file);

            while (scanner.hasNext()) {
                int i = 0;
                String data = scanner.nextLine();

                if (data.indexOf(DEFAULT_SEPARATOR) > -1) {
                    List<String> line = parseLine(data);
                    if (line.size() == 5) {
                        ProductDTO prod = new ProductDTO();
                        prod.setCode(line.get(i++));
                        prod.setName(line.get(i++));
                        prod.setBrand(line.get(i++));
                        prod.setCategory(line.get(i++));
                        prod.setPrice(new BigDecimal(NumberUtils.extractPrice(line.get(i))));

                        ServiceResponse res = validate(prod);
                        if (res.isOK()) {
                            prodList.add(prod);
                            prodCount++;
                        } else {
                            ImportProblem ip = new ImportProblem("Line: " + row + ", Code: " + prod.getCode(), res.getProblems());
                            report.getProblems().add(ip);
                        }
                    } else {
                        ImportProblem ip = new ImportProblem("Line: " + row + ": Invalid format. There must be 5 fields in each row!");
                        report.getProblems().add(ip);
                    }
                } else {
                    formatError = true;
                    break;
                }

                //todo: must be limited with workspace's plan limit
                if (prodCount > 100) {
                    break;
                }

                row++;
            }
            scanner.close();

            if (formatError) {
                report.setStatus(HttpStatus.BAD_REQUEST_400);
                report.setResult("Format error! Rules: Header line isn't allowed. Separator must be " + DEFAULT_SEPARATOR + " and allowed Quote can be " + DEFAULT_QUOTE);
            } else {
                if (report.getProblems() == null || report.getProblems().size() == 0) {
                    ServiceResponse res = repository.bulkInsert(report, prodList);
                    if (res.isOK()) {
                        report.setResult("CSV file has been successfully uploaded.");
                    } else {
                        report.setStatus(HttpStatus.BAD_REQUEST_400);
                        report.setResult(res.getResult());
                    }
                } else {
                    report.setStatus(HttpStatus.BAD_REQUEST_400);
                    report.setResult("Failed to upload CSV file, please see details.");
                }
            }
        } catch (Exception e) {
            log.error("Failed to import a csv file.", e);
            report = new ImportReport(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
        }
        return report;
    }

    private static List<String> parseLine(String cvsLine) {
        List<String> result = new ArrayList<>();

        if (cvsLine == null || cvsLine.isEmpty()) {
            return result;
        }

        StringBuffer curVal = new StringBuffer();
        boolean inQuotes = false;
        boolean startCollectChar = false;
        boolean doubleQuotesInColumn = false;
        char[] chars = cvsLine.toCharArray();

        for (char ch : chars) {
            if (inQuotes) {
                startCollectChar = true;
                if (ch == DEFAULT_QUOTE) {
                    inQuotes = false;
                    doubleQuotesInColumn = false;
                } else {
                    //Fixed : allow "" in custom quote enclosed
                    if (ch == '\"') {
                        if (!doubleQuotesInColumn) {
                            curVal.append(ch);
                            doubleQuotesInColumn = true;
                        }
                    } else {
                        curVal.append(ch);
                    }
                }
            } else {
                if (ch == DEFAULT_QUOTE) {
                    inQuotes = true;
                    //Fixed : allow "" in empty quote enclosed
                    if (chars[0] != '"') {
                        curVal.append('"');
                    }
                    //double quotes in column will hit this!
                    if (startCollectChar) {
                        curVal.append('"');
                    }
                } else if (ch == DEFAULT_SEPARATOR) {
                    result.add(curVal.toString());
                    curVal = new StringBuffer();
                    startCollectChar = false;
                } else if (ch == '\r') {
                    //ignore LF characters
                } else if (ch == '\n') {
                    //the end, break!
                    break;
                } else {
                    curVal.append(ch);
                }
            }

        }
        result.add(curVal.toString());

        return result;
    }

    private ServiceResponse validate(ProductDTO productDTO) {
        List<Problem> problems = new ArrayList<>();

        if (StringUtils.isBlank(productDTO.getCode().trim())) {
            problems.add(new Problem("code", "Product code cannot be null!"));
        } else if (productDTO.getCode().length() < 2 || productDTO.getCode().length() > 120) {
            problems.add(new Problem("code", "Product code must be between 2 and 120 chars!"));
        }

        if (StringUtils.isBlank(productDTO.getName().trim())) {
            problems.add(new Problem("name", "Product name cannot be null!"));
        } else if (productDTO.getName().length() < 3 || productDTO.getName().length() > 500) {
            problems.add(new Problem("name", "Product name must be between 3 and 500 chars!"));
        }

        if (! StringUtils.isBlank(productDTO.getBrand()) && productDTO.getBrand().length() > 100) {
            problems.add(new Problem("brand", "Brand can be up to 100 chars!"));
        }

        if (! StringUtils.isBlank(productDTO.getCategory()) && productDTO.getCategory().length() > 100) {
            problems.add(new Problem("category", "Category can be up to 100 chars!"));
        }

        if (productDTO.getPrice() == null || productDTO.getPrice().compareTo(BigDecimal.ONE) < 0) {
            problems.add(new Problem("price", "Price must be greater than zero!"));
        }

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

}
