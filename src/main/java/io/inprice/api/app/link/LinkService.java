package io.inprice.api.app.link;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;

import io.inprice.api.app.link.dto.LinkDTO;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;

class LinkService {

  private final UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });

  Response insert(LinkDTO dto) {
    if (dto != null) {
      Response res = validate(dto);

      if (res.isOK()) {
        try (Handle handle = Database.getHandle()) {
          LinkDao linkDao = handle.attach(LinkDao.class);

          String urlHash = DigestUtils.md5Hex(dto.getUrl());
          Link link = linkDao.findByProductIdAndUrlHash(dto.getProductId(), urlHash);
          if (link == null) {

            Link sample = linkDao.findByUrlHash(urlHash);
            if (sample != null) { // if any, lets clone it
              long id = linkDao.insert(link, dto.getProductId(), CurrentUser.getCompanyId());
              if (id > 0) {
                sample.setId(id);
                res = new Response(sample);
              }
            } else {
              long id = linkDao.insert(dto.getUrl(), urlHash, dto.getProductId(), CurrentUser.getCompanyId());
              if (id > 0) {
                sample = new Link();
                sample.setId(id);
                sample.setUrl(dto.getUrl());
                sample.setUrlHash(urlHash);
                sample.setProductId(dto.getProductId());
                res = new Response(sample);
              }
            }

          } else {
            res = Responses.DataProblem.ALREADY_EXISTS;
          }
        }
      }
      return res;
    }
    return Responses.Invalid.LINK;
  }

  Response deleteById(Long id) {
    if (id != null && id > 0) {
      final boolean[] isOK = { false };
      final String where = String.format("where link_id=%d and company_id=%d; ", id, CurrentUser.getCompanyId());

      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(h -> {
          Batch batch = h.createBatch();
          batch.add("delete from link_price " + where);
          batch.add("delete from link_history " + where);
          batch.add("delete from link_spec " + where);
          batch.add("delete from link " + where.replace("link_", ""));
          batch.execute();
          isOK[0] = true;
          return isOK[0];
        });
      }

      if (isOK[0]) {
        return Responses.OK;
      }
    }
    return Responses.NotFound.LINK;
  }

  Response changeStatus(Long id, LinkStatus newStatus) {
    final Response[] res = { Responses.NotFound.LINK };

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {

        handle.inTransaction(h -> {
          LinkDao linkDao = handle.attach(LinkDao.class);
        
          Link link = linkDao.findById(id);
          if (link != null) {

            if (link.getCompanyId().equals(CurrentUser.getCompanyId())) {
              if (!link.getStatus().equals(newStatus)) {

                boolean suitable = false;

                switch (link.getStatus()) {
                  case AVAILABLE: {
                    suitable = (newStatus.equals(LinkStatus.TOBE_RENEWED) || newStatus.equals(LinkStatus.PAUSED));
                    break;
                  }
                  case PAUSED: {
                    suitable = (newStatus.equals(LinkStatus.RESUMED));
                    break;
                  }
                  case TOBE_CLASSIFIED:
                  case TOBE_RENEWED:
                  case TOBE_IMPLEMENTED:
                  case IMPLEMENTED:
                  case NOT_AVAILABLE:
                  case READ_ERROR:
                  case SOCKET_ERROR:
                  case NETWORK_ERROR:
                  case CLASS_PROBLEM:
                  case INTERNAL_ERROR: {
                    suitable = (newStatus.equals(LinkStatus.PAUSED));
                    break;
                  }
                  default:
                }

                if (suitable) {
                  boolean isOK = linkDao.changeStatus(id, newStatus.name(), link.getCompanyId());
                  if (isOK) {
                    res[0] = Responses.OK;
                  }
                }

              }
            }
            if (! res[0].isOK()) res[0] = Responses.DataProblem.NOT_SUITABLE;
          } else {
            res[0] = Responses.NotFound.LINK;
          }

          return res[0].isOK();
        });
      }
    }

    return res[0];
  }

  private Response validate(LinkDTO dto) {
    String problem = null;

    if (!urlValidator.isValid(dto.getUrl())) {
      problem = "Invalid URL!";
    }

    if (problem == null) {
      if (dto.getProductId() == null || dto.getProductId() < 1) {
        problem = "Product id cannot be null!";
      }
    }

    if (problem == null) {
      dto.setUrl(SqlHelper.clear(dto.getUrl()));
      return Responses.OK;
    } else {
      return new Response(problem);
    }
  }

}
