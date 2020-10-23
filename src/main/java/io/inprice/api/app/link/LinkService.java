package io.inprice.api.app.link;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.link.dto.LinkDTO;
import io.inprice.api.app.link.dto.LinkSearchDTO;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;

class LinkService {

  private static final Logger log = LoggerFactory.getLogger(LinkService.class);

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

            Link sample = linkDao.findSampleByUrlHashAndStatus(urlHash, LinkStatus.AVAILABLE.name());
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

  public Response fullSearch(LinkSearchDTO dto) {
    clearSearchDto(dto);

    //---------------------------------------------------
    //building the criteria up
    //---------------------------------------------------
    StringBuilder criteria = new StringBuilder();

    criteria.append("where l.company_id = ");
    criteria.append(CurrentUser.getCompanyId());

    if (StringUtils.isNotBlank(dto.getTerm())) {
      criteria.append(" and l.sku like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or l.name like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or l.brand like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or l.seller like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or s.name like '%"); // platform!
      criteria.append(dto.getTerm());
      criteria.append("%' ");
    }

    if (dto.getStatuses() != null && dto.getStatuses().length > 0) {
      criteria.append(
        String.format(" and l.status in ('%s') ", String.join("', '", dto.getStatuses()))
      );
    }

    //limiting
    String limit = " limit " + Consts.ROW_LIMIT_FOR_LISTS;
    if (dto.getLoadMore() && dto.getRowCount() >= Consts.ROW_LIMIT_FOR_LISTS) {
      limit = " limit " + dto.getRowCount() + ", " + Consts.ROW_LIMIT_FOR_LISTS;
    }

    //---------------------------------------------------
    //fetching the data
    //---------------------------------------------------
    try (Handle handle = Database.getHandle()) {
      List<Link> searchResult =
        handle.createQuery(
          "select *, s.name as platform from link as l " + 
          "left join site as s on s.id = l.site_id " +
          criteria +
          " order by l.status, s.name, l.last_update " +
          limit
        )
      .map(new LinkMapper())
      .list();

      return new Response(Collections.singletonMap("rows", searchResult));
    } catch (Exception e) {
      log.error("Failed in full search for links.", e);
      return Responses.ServerProblem.EXCEPTION;
    }
  }

  Response deleteById(Long id) {
    if (id != null && id > 0) {
      final boolean[] isOK = { false };
      final String where = String.format("where link_id=%d and company_id=%d; ", id, CurrentUser.getCompanyId());

      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transactional -> {
          Batch batch = transactional.createBatch();
          batch.add("delete from link_price " + where);
          batch.add("delete from link_history " + where);
          batch.add("delete from link_spec " + where);
          batch.add("delete from link " + where.replace("link_", "")); //important!!!
          int[] result = batch.execute();
          isOK[0] = result[3] > 0;
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

        handle.inTransaction(transactional -> {
          LinkDao linkDao = handle.attach(LinkDao.class);
        
          Link link = linkDao.findById(id);
          if (link != null) {

            if (link.getCompanyId().equals(CurrentUser.getCompanyId())) {
              if (!link.getStatus().equals(newStatus)) {

                boolean suitable = false;

                switch (link.getStatus()) {
                  case AVAILABLE: {
                    suitable = (newStatus.equals(LinkStatus.PAUSED));
                    break;
                  }
                  case PAUSED: {
                    suitable = (newStatus.equals(LinkStatus.RESUMED));
                    break;
                  }
                  case TOBE_CLASSIFIED:
                  case TOBE_IMPLEMENTED:
                  case NOT_AVAILABLE:
                  case NETWORK_ERROR:
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

  private void clearSearchDto(LinkSearchDTO dto) {
    dto.setTerm(SqlHelper.clear(dto.getTerm()));
    if (dto.getStatuses() != null && dto.getStatuses().length > 0) {
      Set<String> newStatusSet = new HashSet<>(dto.getStatuses().length);
      Set<String> linkNamesSet = EnumUtils.getEnumMap(LinkStatus.class).keySet();
	
      for (int i = 0; i < dto.getStatuses().length; i++) {
        String status = dto.getStatuses()[i];
        if (StringUtils.isNotBlank(status)) {
          if (linkNamesSet.contains(status)) newStatusSet.add(status);
        }
      }
      dto.setStatuses(newStatusSet.toArray(new String[0]));
    }
  }

}
