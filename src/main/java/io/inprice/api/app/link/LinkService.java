package io.inprice.api.app.link;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.mappers.LinkMapper;
import io.inprice.common.meta.LinkStatus;
import io.inprice.common.models.Link;
import io.inprice.common.models.LinkHistory;
import io.inprice.common.repository.CommonRepository;
import io.inprice.common.utils.DateUtils;

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
              long id = linkDao.insert(link, dto.getProductId(), CurrentUser.getAccountId());
              if (id > 0) {
                sample.setId(id);
                linkDao.insertHistory(sample);
                res = new Response(sample);
              }
            } else {
              long id = linkDao.insert(dto.getUrl(), urlHash, dto.getProductId(), CurrentUser.getAccountId());
              if (id > 0) {
                sample = new Link();
                sample.setId(id);
                sample.setUrl(dto.getUrl());
                sample.setUrlHash(urlHash);
                sample.setProductId(dto.getProductId());
                sample.setAccountId(CurrentUser.getAccountId());
                linkDao.insertHistory(sample);
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

    criteria.append("where l.import_detail_id is null and l.account_id = ");
    criteria.append(CurrentUser.getAccountId());

    if (StringUtils.isNotBlank(dto.getTerm())) {
      criteria.append(" and l.sku like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or l.name like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or l.brand like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or l.seller like '%");
      criteria.append(dto.getTerm());
      criteria.append("%' or plt.domain like '%");
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
          "select *, plt.domain as platform from link as l " + 
      		"left join platform as plt on plt.id = l.platform_id " + 
          criteria +
          " order by l.status, plt.domain, l.last_update " +
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
      final String where = String.format("where link_id=%d and account_id=%d; ", id, CurrentUser.getAccountId());

      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transactional -> {
          LinkDao linkDao = transactional.attach(LinkDao.class);

          Link deletedLink = linkDao.findById(id);
          if (deletedLink != null) {
            Batch batch = transactional.createBatch();
            batch.add("delete from link_price " + where);
            batch.add("delete from link_history " + where);
            batch.add("delete from link_spec " + where);
            batch.add("delete from link " + where.replace("link_", "")); //important!!!
            int[] result = batch.execute();
            isOK[0] = result[3] > 0;

            if (isOK[0] && LinkStatus.AVAILABLE.equals(deletedLink.getStatus())) {
              CommonRepository.adjustProductPrice(transactional, deletedLink.getProductId(), null);
            }
          }

          return isOK[0];
        });
      }

      if (isOK[0]) {
        return Responses.OK;
      }
    }
    return Responses.NotFound.LINK;
  }

  Response toggleStatus(Long id) {
    Response[] res = { Responses.NotFound.LINK };

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transactional -> {
          LinkDao linkDao = transactional.attach(LinkDao.class);

          Link link = linkDao.findById(id);
          if (link != null) {

            //check if he tries too much
            List<LinkHistory> lastThreeList = linkDao.findLastThreeHistoryRowsByLinkId(id);
            if (lastThreeList.size() == 3) {
              LinkHistory row0 = lastThreeList.get(0);
              LinkHistory row2 = lastThreeList.get(2);
              if (row0.getStatus().equals(row2.getStatus())) {
                Date now = new Date();
                long diff0 = DateUtils.findDayDiff(row0.getCreatedAt(), now);
                long diff2 = DateUtils.findDayDiff(row2.getCreatedAt(), now);
                if (diff0 == 0 && diff2 == 0) {
                  res[0] = Responses.DataProblem.TOO_MANY_TOGGLING;
                }
              }
            }

            if (! res[0].equals(Responses.DataProblem.TOO_MANY_TOGGLING)) {
              LinkStatus newStatus = (LinkStatus.PAUSED.equals(link.getStatus()) ? link.getPreStatus() : LinkStatus.PAUSED);
              boolean isOK = linkDao.toggleStatus(id, newStatus.name());
              if (isOK) {
                link.setPreStatus(link.getStatus());
                link.setStatus(newStatus);
                long historyId = linkDao.insertHistory(link);
                if (historyId > 0) {
                  if (LinkStatus.AVAILABLE.equals(newStatus)) {
                    CommonRepository.adjustProductPrice(transactional, link.getProductId(), null);
                  }
                res[0] = Responses.OK;
                }
              }
            }

          }
          return res[0].isOK();
        });
      }
    }

    return res[0];
  }

  Response getDetails(Long id) {
    Response res = Responses.NotFound.LINK;

    if (id != null && id > 0) {
      try (Handle handle = Database.getHandle()) {
        LinkDao linkDao = handle.attach(LinkDao.class);

        Link link = linkDao.findById(id);
        if (link != null) {

          Map<String, Object> data = new HashMap<>(4);
          List<LinkHistory> historyList = linkDao.findHistoryListByLinkId(id);
          if (historyList != null && historyList.size() > 0) {
            data.put("link", link);
            data.put("historyList", historyList);
            data.put("priceList", linkDao.findPriceListByLinkId(id));
            data.put("specList", linkDao.findSpecListByLinkId(id));
            res = new Response(data);
          }
        }
      }
    }

    return res;
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
