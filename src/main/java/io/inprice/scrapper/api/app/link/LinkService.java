package io.inprice.scrapper.api.app.link;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.external.RabbitMQ;
import io.inprice.scrapper.api.info.SearchModel;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.utils.URLUtils;
import io.inprice.scrapper.common.helpers.Beans;
import io.inprice.scrapper.common.meta.LinkStatus;
import io.inprice.scrapper.common.models.Link;

public class LinkService {

  private final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);
  private final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);

  public ServiceResponse findById(Long id) {
    return linkRepository.findById(id);
  }

  public ServiceResponse getList(Long prodId) {
    if (prodId == null || prodId < 1) return Responses.NotFound.PRODUCT;

    ServiceResponse found = productRepository.findById(prodId);
    if (found.isOK()) {
      return linkRepository.getList(found.getData());
    }

    return Responses.NotFound.PRODUCT;
  }

  public ServiceResponse search(Map<String, String> searchMap) {
    SearchModel sm = new SearchModel(searchMap, "name, platform, seller", Link.class);
    sm.setQuery("select l.*, s.name as platform from link as l left join site as s on s.id = l.site_id");
    sm.setFields(Arrays.asList("seller", "s.name"));

    String whereStatus = searchMap.get("status");
    if (StringUtils.isNotBlank(whereStatus) && ! whereStatus.equals("null")) {
      try {
        whereStatus = "status = '" + LinkStatus.valueOf(whereStatus).name() + "'";
      } catch (Exception ignored) {}
    }

    return linkRepository.search(sm, whereStatus);
  }

  public ServiceResponse insert(LinkDTO dto) {
    if (dto != null) {
      ServiceResponse res = validate(dto);
      if (res.isOK()) {
        res = linkRepository.insert(dto);
      }
      return res;
    }
    return Responses.Invalid.LINK;
  }

  public ServiceResponse deleteById(Long linkId) {
    if (linkId != null && linkId > 0) {
      ServiceResponse res = linkRepository.findById(linkId);
      if (res.isOK()) {
        ServiceResponse del = linkRepository.deleteById(linkId);
        if (del.isOK()) {
          // inform the product to be refreshed
          Link link = res.getData();
          RabbitMQ.publish(Props.MQ_EXCHANGE_CHANGES(), Props.MQ_ROUTING_DELETED_LINKS(), link.getProductId());
          return Responses.OK;
        }
      }
      return Responses.NotFound.LINK;
    } else {
      return Responses.Invalid.LINK;
    }
  }

  @SuppressWarnings("incomplete-switch")
  public ServiceResponse changeStatus(Long id, LinkStatus status) {
    if (id == null || id < 1) {
      return Responses.NotFound.LINK;
    }

    ServiceResponse res = linkRepository.findById(id);
    if (res.isOK()) {
      Link link = res.getData();

      if (!link.getCompanyId().equals(CurrentUser.getCompanyId())) {
        return Responses.Invalid.PRODUCT;
      }

      if (link.getStatus().equals(status)) {
        return Responses.DataProblem.NOT_SUITABLE;
      }

      boolean suitable = false;

      switch (link.getStatus()) {
        case AVAILABLE: {
          suitable = (status.equals(LinkStatus.RENEWED) || status.equals(LinkStatus.PAUSED));
          break;
        }
        case PAUSED: {
          suitable = (status.equals(LinkStatus.RESUMED));
          break;
        }
        case NEW:
        case RENEWED:
        case BE_IMPLEMENTED:
        case IMPLEMENTED:
        case NOT_AVAILABLE:
        case READ_ERROR:
        case SOCKET_ERROR:
        case NETWORK_ERROR:
        case CLASS_PROBLEM:
        case INTERNAL_ERROR: {
          suitable = (status.equals(LinkStatus.PAUSED));
          break;
        }
      }
      if (!suitable)
        return Responses.DataProblem.NOT_SUITABLE;

      return linkRepository.changeStatus(id, link.getProductId(), status);
    }

    return res;
  }

  private ServiceResponse validate(LinkDTO dto) {
    String problem = null;

    if (!URLUtils.isAValidURL(dto.getUrl())) {
      problem = "Invalid URL!";
    } else if (dto.getUrl().length() > 1024) {
      problem = "The length of URL must be less than 1024 chars!";
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
