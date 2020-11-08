package io.inprice.api.app.imbort;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.company.CompanyDao;
import io.inprice.api.app.link.LinkDao;
import io.inprice.api.app.product.ProductDao;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.Props;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SiteFinder;
import io.inprice.common.meta.ImportType;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.Link;
import io.inprice.common.models.Product;
import io.inprice.common.models.Site;
import io.inprice.common.utils.URLUtils;

public class URLImportService extends BaseImportService {

  private static final Logger log = LoggerFactory.getLogger(URLImportService.class);

  private static final String ASIN_REGEX = "^(?i)(B0|BT)[0-9A-Z]{8}$";
  private static final String SKU_REGEX = "^[1-3][0-9]{10,11}$";

  @Override
  Response upload(ImportType importType, String content) {
    if (StringUtils.isBlank(content) || content.length() < 10) { // byte
      return Responses.Upload.EMPTY;
    }

    String identifier = null;
    final String[] regex = { "" };

    Site ebaySite = SiteFinder.findSiteByUrl(Props.PREFIX_FOR_SEARCH_EBAY());
    Site amazonSite = SiteFinder.findSiteByUrl(Props.PREFIX_FOR_SEARCH_AMAZON());

    switch (importType) {
      case EBAY: {
        identifier = "SKU list";
        regex[0] = SKU_REGEX;
        break;
      }
      case AMAZON: {
        identifier = "ASIN list";
        regex[0] = ASIN_REGEX;
        break;
      }
      case URL: {
        identifier = "URL list";
        regex[0] = URLUtils.URL_CHECK_REGEX;
        break;
      }
      case CSV: {
        return Responses.Invalid.DATA;
      }
    }

    Response[] res = { Responses.OK };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transactional -> {

        ImportDao importDao = transactional.attach(ImportDao.class);
        long importId = importDao.insert(importType.name(), CurrentUser.getCompanyId());
        if (importId == 0) {
          res[0] = Responses.DataProblem.DB_PROBLEM;
          return false;
        }

        CompanyDao companyDao = transactional.attach(CompanyDao.class);

        Company company = companyDao.findById(CurrentUser.getCompanyId());
        int allowedCount = company.getProductLimit() - company.getProductCount();

        Set<String> insertedSet = new HashSet<>();
        if (allowedCount > 0) {

          int actualCount = company.getProductCount();
          if (actualCount < allowedCount) {

            try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
              LinkDao linkDao = transactional.attach(LinkDao.class);
              ProductDao productDao = transactional.attach(ProductDao.class);

              String line;
              while ((line = reader.readLine()) != null) {
                if (StringUtils.isBlank(line) || line.trim().startsWith("#")) {
                  continue;
                }

                String url = null;
                String problem = null;
                Site site = null;
                LinkStatus status = LinkStatus.TOBE_CLASSIFIED;

                if (actualCount < allowedCount) {
                  boolean exists = insertedSet.contains(line);
                  if (! exists) {

                    if (line.matches(regex[0])) {
                      if (ImportType.URL.equals(importType)) {
                        Link link = linkDao.findByUrlHashForImport(DigestUtils.md5Hex(line), CurrentUser.getCompanyId());
                        if (link != null) {
                          status = LinkStatus.DUPLICATE;
                        }
                      } else {
                        Product product = productDao.findByCode(line, CurrentUser.getCompanyId());
                        if (product != null) {
                          status = LinkStatus.DUPLICATE;
                        }
                      }

                      if (status.equals(LinkStatus.TOBE_CLASSIFIED)) {
                        switch (importType) {
                          case EBAY: {
                            url = Props.PREFIX_FOR_SEARCH_EBAY() + line;
                            site = ebaySite;
                            break;
                          }
                          case AMAZON: {
                            url = Props.PREFIX_FOR_SEARCH_AMAZON() + line;
                            site = amazonSite;
                            break;
                          }
                          default:
                            url = line;
                            site = SiteFinder.findSiteByUrl(url);
                            break;
                        }

                      } else {
                        status = LinkStatus.DUPLICATE;
                        problem = "Already defined!";
                      }
                    } else {
                      status = LinkStatus.IMPROPER;
                      problem = "Doesn't match the rules!";
                    }
                  } else {
                    status = LinkStatus.IMPORTED;
                    problem = "Already imported!";
                  }
                } else {
                  status = LinkStatus.LIMIT_EXCEEDED;
                  problem = "Plan limit exceeded!";
                }

                if (site == null && status.equals(LinkStatus.TOBE_CLASSIFIED)) {
                  status = LinkStatus.TOBE_IMPLEMENTED;
                }

                linkDao.importProduct(
                  url, 
                  DigestUtils.md5Hex(url), 
                  status.name(), 
                  problem, 
                  (problem != null ? 1 : 0),
                  (site != null ? site.getClassName() : null),
                  (site != null ? site.getDomain() : null),
                  importId,
                  CurrentUser.getCompanyId()
                );
              }
            }
          } else {
            res[0] = new Response("You have reached your plan limit. Please select a broader plan.");
          }
        } else {
          res[0] = new Response("Seems you haven't chosen a plan yet. Please consider buying a plan.");
        }

        return insertedSet.size() > 0;
      });
    } catch (Exception e) {
      log.error("An error occurred during importing " + identifier, e);
      res[0] = Responses.ServerProblem.EXCEPTION;
    }

    return res[0];
  }

}