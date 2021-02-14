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
import io.inprice.api.app.product.dto.ProductDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.Props;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.ImportType;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.ImportDetail;
import io.inprice.common.models.Link;
import io.inprice.common.models.Platform;
import io.inprice.common.repository.PlatformRepository;
import io.inprice.common.utils.URLUtils;

class URLImportService extends BaseImportService {

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

    Platform[] ebaySite = { null };
    Platform[] amazonSite = { null };

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

              String line;
              while ((line = reader.readLine()) != null) {
                if (StringUtils.isBlank(line) || line.trim().startsWith("#")) {
                  continue;
                }

                String url = null;
                String problem = null;
                Platform platform = null;
                Link similar = null;
                LinkStatus status = LinkStatus.TOBE_IMPLEMENTED;

                if (actualCount < allowedCount) {
                  boolean exists = insertedSet.contains(line);
                  if (! exists) {

                    if ((! ImportType.URL.equals(importType) && line.matches(regex[0])) || URLUtils.isAValidURL(line)) {
  
                      switch (importType) {
                        case EBAY: {
                          if (ebaySite[0] == null) {
                            ebaySite[0] = PlatformRepository.findByUrl(handle, Props.PREFIX_FOR_SEARCH_EBAY());
                          }
                          url = Props.PREFIX_FOR_SEARCH_EBAY() + line;
                          platform = ebaySite[0];
                          status = LinkStatus.TOBE_CLASSIFIED;
                          break;
                        }
                        case AMAZON: {
                          if (amazonSite[0] == null) {
                            amazonSite[0] = PlatformRepository.findByUrl(handle, Props.PREFIX_FOR_SEARCH_AMAZON());
                          }
                          url = Props.PREFIX_FOR_SEARCH_AMAZON() + line;
                          platform = amazonSite[0];
                          status = LinkStatus.TOBE_CLASSIFIED;
                          break;
                        }
                        default:
                          url = line;
                          platform = PlatformRepository.findByUrl(handle, url);
                          if (platform != null) {
                            if (platform.getStatus() != null) {
                              status = platform.getStatus();
                              problem = platform.getProblem();
                            } else {
                              status = LinkStatus.TOBE_CLASSIFIED;
                            }
                          }
                          break;
                      }

                      if (status.equals(LinkStatus.TOBE_CLASSIFIED)) { //which means the link is suitable
                        List<Link> linkList = linkDao.findByUrlHashForImport(DigestUtils.md5Hex(url)); // find similar links previously added
                        if (linkList != null && linkList.size() > 0) {
                          for (Link link: linkList) {
                            if (link.getAccountId().longValue() != accountId) { // if it belongs to another account
                              if (StringUtils.isNotBlank(link.getSku()) && similar == null) { // one time is enough to clone
                                similar = link;
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
                impdet.setStatus(problem != null ? problem : "TOBE CLASSIFIED");
                impdet.setImportId(importId);
                impdet.setAccountId(accountId);
                long importDetailId = importDao.insertDetail(impdet);

                if (similar == null && problem == null) {
                  linkDao.importProduct(
                    url, 
                    DigestUtils.md5Hex(url), 
                    status.name(), 
                    problem, 
                    (problem != null ? 1 : 0),
                    (platform != null ? platform.getId() : null),
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
          //res[0] = Responses.Illegal.INCOMPATIBLE_CONTENT;
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