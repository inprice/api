package io.inprice.api.app.competitor;

import org.apache.commons.validator.routines.UrlValidator;

import io.inprice.api.app.product.ProductRepository;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.CompetitorDTO;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.CompetitorStatus;
import io.inprice.common.models.Competitor;

public class CompetitorService {

  private final CompetitorRepository competitorRepository = Beans.getSingleton(CompetitorRepository.class);
  private final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);

  private final UrlValidator urlValidator = new UrlValidator(new String[] {"http", "https"});

  public ServiceResponse findById(Long id) {
    return competitorRepository.findById(id);
  }

  public ServiceResponse search(String term) {
    return competitorRepository.search(term);
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
        //Competitor oldOne = res.getData();
        //TODO: commonDao kanalı ile yapılmalı
        //ServiceResponse delRes = competitorRepository.deleteById(competitorId);
        return res;
      }
    }
    return Responses.NotFound.COMPETITOR;
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

    if (! urlValidator.isValid(dto.getUrl())) {
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
