package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ImportProblem;
import io.inprice.scrapper.api.info.ImportReport;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.api.rest.validator.ProductDTOValidator;
import io.inprice.scrapper.common.meta.FileType;
import io.inprice.scrapper.common.models.ProductImport;
import io.inprice.scrapper.common.models.ProductImportRow;
import io.inprice.scrapper.common.utils.NumberUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProductCSVImportService {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);

    private final ProductRepository repository = Beans.getSingleton(ProductRepository.class);

    private static final char DEFAULT_SEPARATOR = ';';
    private static final char DEFAULT_QUOTE = '"';

    public ImportReport upload(String file) {
        ProductImport imbort = new ProductImport();
        imbort.setFileType(FileType.CSV);
        imbort.setCompanyId(Context.getCompanyId());
        imbort.setWorkspaceId(Context.getWorkspaceId());

        List<ProductImportRow> importRowList = new ArrayList<>();
        List<ProductDTO> prodList = new ArrayList<>();

        try {
            int row = 1;
            int prodCount = repository.findProductCount();

            if (prodCount > -1) {
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

                            ServiceResponse res = ProductDTOValidator.validate(prod);
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
                    }

                    //todo: must be limited with workspace's plan limit
                    if (prodCount > 100) {
                        break;
                    }

                    row++;
                }
                scanner.close();
            }

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

}
