package io.inprice.scrapper.api.app.link;

import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.external.RabbitMQ;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.utils.URLUtils;

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

  public ServiceResponse insert(LinkDTO dto) {
    if (dto != null) {
      ServiceResponse res = validate(dto, true);
      if (res.isOK()) {
        res = linkRepository.insert(dto);
      }
      return res;
    }
    return Responses.Invalid.LINK;
  }

  public ServiceResponse update(LinkDTO dto) {
    if (dto != null) {
      ServiceResponse res = validate(dto, false);
      if (res.isOK()) {
        res = linkRepository.update(dto);
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
          RabbitMQ.publish(Props.getMQ_ChangeExchange(), Props.getRouterKey_DeletedLinks(), link.getProductId());
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

  private ServiceResponse validate(LinkDTO dto, boolean isInsert) {
    String problem = null;

    if (!URLUtils.isAValidURL(dto.getUrl())) {
      problem = "Invalid URL!";
    } else if (dto.getUrl().length() > 2000) {
      problem = "The length of URL must be less than 2000 chars!";
    }

    if (problem == null && !isInsert && (dto.getId() == null || dto.getId() < 1)) {
      problem = "Link id cannot be null!";
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
