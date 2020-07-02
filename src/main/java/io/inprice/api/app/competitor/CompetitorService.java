package io.inprice.api.app.competitor;

import java.util.Arrays;
import java.util.Map;

import com.rabbitmq.client.Channel;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.app.product.ProductRepository;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CompetitorDTO;
import io.inprice.api.info.SearchModel;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.RabbitMQ;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.CompetitorStatus;
import io.inprice.common.models.Competitor;
import io.inprice.common.utils.URLUtils;

public class CompetitorService {

  private final CompetitorRepository competitorRepository = Beans.getSingleton(CompetitorRepository.class);
  private final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);

  public ServiceResponse findById(Long id) {
    return competitorRepository.findById(id);
  }

  public ServiceResponse getList(Long prodId) {
    if (prodId == null || prodId < 1) return Responses.NotFound.PRODUCT;

    ServiceResponse found = productRepository.findById(prodId);
    if (found.isOK()) {
      return competitorRepository.getList(found.getData());
    }

    return Responses.NotFound.PRODUCT;
  }

  public ServiceResponse search(Map<String, String> searchMap) {
    SearchModel sm = new SearchModel(searchMap, "name, platform, seller", Competitor.class);
    sm.setQuery("select l.*, s.name as platform from competitor as l left join site as s on s.id = l.site_id");
    sm.setFields(Arrays.asList("seller", "s.name"));

    String whereStatus = searchMap.get("status");
    if (StringUtils.isNotBlank(whereStatus) && ! whereStatus.equals("null")) {
      try {
        whereStatus = "status = '" + CompetitorStatus.valueOf(whereStatus).name() + "'";
      } catch (Exception ignored) {}
    }

    return competitorRepository.search(sm, whereStatus);
  }

  public ServiceResponse insert(CompetitorDTO dto) {
    if (dto != null) {
      ServiceResponse res = validate(dto);
      if (res.isOK()) {
        res = competitorRepository.insert(dto);
      }
      return res;
    }
    return Responses.Invalid.COMPETITOR;
  }

  public ServiceResponse deleteById(Long competitorId) {
    if (competitorId != null && competitorId > 0) {
      ServiceResponse res = competitorRepository.findById(competitorId);
      if (res.isOK()) {
        ServiceResponse del = competitorRepository.deleteById(competitorId);
        if (del.isOK()) {
          // inform the product to be refreshed
          Competitor competitor = res.getData();
          Channel channel = RabbitMQ.openChannel();
          RabbitMQ.publish(channel, SysProps.MQ_CHANGES_EXCHANGE(), SysProps.MQ_PRICE_REFRESH_ROUTING(), competitor.getProductId().toString());
          RabbitMQ.closeChannel(channel);
          return Responses.OK;
        }
      }
      return Responses.NotFound.COMPETITOR;
    } else {
      return Responses.Invalid.COMPETITOR;
    }
  }

  @SuppressWarnings("incomplete-switch")
  public ServiceResponse changeStatus(Long id, CompetitorStatus status) {
    if (id == null || id < 1) {
      return Responses.NotFound.COMPETITOR;
    }

    ServiceResponse res = competitorRepository.findById(id);
    if (res.isOK()) {
      Competitor competitor = res.getData();

      if (!competitor.getCompanyId().equals(CurrentUser.getCompanyId())) {
        return Responses.Invalid.PRODUCT;
      }

      if (competitor.getStatus().equals(status)) {
        return Responses.DataProblem.NOT_SUITABLE;
      }

      boolean suitable = false;

      switch (competitor.getStatus()) {
        case AVAILABLE: {
          suitable = (status.equals(CompetitorStatus.TOBE_RENEWED) || status.equals(CompetitorStatus.PAUSED));
          break;
        }
        case PAUSED: {
          suitable = (status.equals(CompetitorStatus.RESUMED));
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
          suitable = (status.equals(CompetitorStatus.PAUSED));
          break;
        }
      }
      if (!suitable)
        return Responses.DataProblem.NOT_SUITABLE;

      return competitorRepository.changeStatus(id, competitor.getProductId(), status);
    }

    return res;
  }

  private ServiceResponse validate(CompetitorDTO dto) {
    String problem = null;

    if (!URLUtils.isAValidURL(dto.getUrl())) {
      problem = "Invalid URL!";
    }

    if (problem == null) {
      if (dto.getProductId() == null || dto.getProductId() < 1) {
        problem = "Product id cannot be null!";
      } else if (!productRepository.findById(dto.getProductId()).isOK()) {
        problem = "Unknown product info!";
      }
    }

    if (problem == null)
      return Responses.OK;
    else
      return new ServiceResponse(problem);
  }

}
