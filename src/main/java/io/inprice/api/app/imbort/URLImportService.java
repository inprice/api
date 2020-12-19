package io.inprice.api.app.imbort;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.link.LinkDao;
import io.inprice.api.app.product.ProductCreator;
import io.inprice.api.app.product.ProductDao;
import io.inprice.api.app.product.dto.ProductDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.Props;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SiteFinder;
import io.inprice.common.info.Site;
import io.inprice.common.meta.ImportType;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.ImportDetail;
import io.inprice.common.models.Link;
import io.inprice.common.models.Product;
import io.inprice.common.utils.URLUtils;

public class URLImportService extends BaseImportService {

  private static final Logger log = LoggerFactory.getLogger(URLImportService.class);

  private static final String ASIN_REGEX = "^(?i)(B0|BT)[0-9A-Z]{8}$";
  private static final String SKU_REGEX = "^[1-3][0-9]{10,11}$";

  @Override
  Response upload(ImportType importType, String content, boolean isFile) {
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

        int successCount = 0;
        int problemCount = 0;
        long accountId = CurrentUser.getAccountId();

        ImportDao importDao = transactional.attach(ImportDao.class);
        long importId = importDao.insert(importType.name(), isFile, accountId);
        if (importId == 0) {
          res[0] = Responses.DataProblem.DB_PROBLEM;
          return false;
        }

        AccountDao accountDao = transactional.attach(AccountDao.class);

        Account account = accountDao.findById(accountId);
        int allowedCount = account.getProductLimit() - account.getProductCount();

        Set<String> insertedSet = new HashSet<>();
        if (allowedCount > 0) {

          int actualCount = account.getProductCount();
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
                Link similar = null;
                LinkStatus status = LinkStatus.RESOLVED;

                if (actualCount < allowedCount) {
                  boolean exists = insertedSet.contains(line);
                  if (! exists) {

                    if ((! ImportType.URL.equals(importType) && line.matches(regex[0])) || URLUtils.isAValidURL(line)) {
  
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
                          if (site == null) status = LinkStatus.TOBE_CLASSIFIED;
                          break;
                      }

                      if (! ImportType.URL.equals(importType)) {
                        Product product = productDao.findByCode(line, accountId);
                        if (product != null) {
                          status = LinkStatus.DUPLICATE;
                        }
                      }

                      if (! status.equals(LinkStatus.DUPLICATE)) {
                        List<Link> linkList = linkDao.findByUrlHashForImport(DigestUtils.md5Hex(url)); // find similar links previously added
                        if (linkList != null && linkList.size() > 0) {
                          for (Link link: linkList) {
                            if (link.getAccountId().longValue() != accountId) { // if it belongs to another account
                              if (StringUtils.isNotBlank(link.getSku()) && similar == null) { // one time is enough to clone
                                similar = link;
                                status = LinkStatus.RESOLVED;
                                problem = null;
                                //we cannot break the loop here since a duplication may occur (see "else" block right below)
                              }
                            } else if (link.getImportDetailId() != null) { // already added
                              similar = null;
                              status = LinkStatus.DUPLICATE;
                              problem = "Already added!";
                              break;
                            }
                          }
                        }
                      } else {
                        status = LinkStatus.DUPLICATE;
                        problem = "Already added!";
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

                if (similar != null) {
                  ProductDTO dto = new ProductDTO();
                  dto.setCode(similar.getSku());
                  dto.setName(similar.getName());
                  dto.setPrice(similar.getPrice());
                  dto.setAccountId(accountId);

                  Response productCreateRes = ProductCreator.create(transactional, dto);
                  if (! productCreateRes.isOK()) {
                    status = LinkStatus.IMPROPER;
                    problem = productCreateRes.getReason();
                  }
                }

                insertedSet.add(line);

                if (problem != null)
                  problemCount++;
                else
                  successCount++;

                ImportDetail impdet = new ImportDetail();
                impdet.setData(line);
                impdet.setEligible(problem == null);
                impdet.setImported(false);
                impdet.setProblem(problem);
                impdet.setImportId(importId);
                impdet.setAccountId(accountId);
                long importDetailId = importDao.insertDetail(impdet);

                // if it is imported then no need to keep it in links table
                if (problem == null && ! impdet.getImported()) {
                  linkDao.importProduct(
                    url, 
                    DigestUtils.md5Hex(url), 
                    status.name(), 
                    problem, 
                    (problem != null ? 1 : 0),
                    (site != null ? site.getClassName() : null),
                    (site != null ? site.getDomain() : null),
                    importDetailId,
                    accountId
                  );
                }
              }
            }
          } else {
            res[0] = new Response("You have reached your plan limit. Please select a broader plan.");
          }
        } else {
          res[0] = new Response("Seems you haven't chosen a plan yet. Please consider buying a plan.");
        }

        boolean isOK = (insertedSet.size() > 0);
        if (isOK) {
          isOK = importDao.updateCounts(importId, successCount, problemCount);
          Map<String, Object> data = new HashMap<>(2);
          data.put("importId", importId);
          data.put("successes", successCount);
          res[0] = new Response(data);
        } else {
          importDao.delete(importId);
          res[0] = Responses.Illegal.INCOMPATIBLE_CONTENT;
        }

        return isOK;
      });
    } catch (Exception e) {
      log.error("Failed to import " + identifier, e);
      res[0] = Responses.ServerProblem.EXCEPTION;
    }

    return res[0];
  }

}