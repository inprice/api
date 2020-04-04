package io.inprice.scrapper.api.app.link;

import io.inprice.scrapper.api.app.product.ProductRepository;
import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.external.RabbitMQ;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.utils.URLUtils;

public class LinkService {

   private final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);
   private final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);

   public ServiceResponse findById(Long id) {
      return linkRepository.findById(id);
   }

   public ServiceResponse getList(Long productId) {
      if (productId == null || productId < 1)
         return Responses.NotFound.PRODUCT;
      return linkRepository.getList(productId);
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
   public ServiceResponse changeStatus(Long id, Long productId, LinkStatus status) {
      if (id == null || id < 1)
         return Responses.NotFound.LINK;
      if (productId == null || productId < 1)
         return Responses.NotFound.PRODUCT;

      ServiceResponse res = linkRepository.findById(id);
      if (res.isOK()) {
         Link link = res.getData();

         if (!link.getProductId().equals(productId)) {
            return Responses.Invalid.PRODUCT;
         }
         if (link.getStatus().equals(status))
            return Responses.DataProblem.NOT_SUITABLE;

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

         return linkRepository.changeStatus(id, productId, status);
      }

      return res;
   }

   private ServiceResponse validate(LinkDTO dto) {
      String problem = null;

      if (!URLUtils.isAValidURL(dto.getUrl())) {
         problem = "Invalid URL!";
      } else if (dto.getUrl().length() > 2000) {
         problem = "The length of URL must be less than 2000 chars!";
      }

      if (problem == null) {
         if (dto.getProductId() == null || dto.getProductId() < 1) {
            problem = "Product cannot be null!";
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
