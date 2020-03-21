package io.inprice.scrapper.api.rest.service;

import java.util.ArrayList;
import java.util.List;

import io.inprice.scrapper.api.config.Props;
import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.RabbitMQ;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.repository.LinkRepository;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.models.Link;
import io.inprice.scrapper.common.utils.URLUtils;

public class LinkService {

    private final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);
    private final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);

    public ServiceResponse findById(Long id) {
        return linkRepository.findById(id);
    }

    public ServiceResponse getList(Long productId) {
        if (productId == null || productId < 1) return Responses.NotFound.PRODUCT;
        return linkRepository.getList(productId);
    }

    public ServiceResponse insert(LinkDTO linkDTO) {
        if (linkDTO != null) {
            ServiceResponse res = validate(linkDTO);
            if (res.isOK()) {
                res = linkRepository.insert(linkDTO);
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
                    //inform the product to be refreshed
                	Link link = res.getData();
                    RabbitMQ.publish(Props.getMQ_ChangeExchange(), Props.getRoutingKey_DeletedLinks(), link.getProductId());
                    return Responses.OK;
                }
            }
            return Responses.NotFound.LINK;
        } else {
            return Responses.Invalid.LINK;
        }
    }

    @SuppressWarnings("incomplete-switch")
    public ServiceResponse changeStatus(Long id, Long productId, Status status) {
        if (id == null || id < 1) return Responses.NotFound.LINK;
        if (productId == null || productId < 1) return Responses.NotFound.PRODUCT;

        ServiceResponse res = linkRepository.findById(id);
        if (res.isOK()) {
            Link link = res.getData();

            if (! link.getProductId().equals(productId)) {
                return Responses.Invalid.PRODUCT;
            }
            if (link.getStatus().equals(status)) return Responses.DataProblem.NOT_SUITABLE;

            boolean suitable = false;

            switch (link.getStatus()) {
                case AVAILABLE: {
                    suitable = (status.equals(Status.RENEWED) || status.equals(Status.PAUSED));
                    break;
                }
                case PAUSED: {
                    suitable = (status.equals(Status.RESUMED));
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
                    suitable = (status.equals(Status.PAUSED));
                    break;
                }
            }
            if (! suitable) return Responses.DataProblem.NOT_SUITABLE;

            return linkRepository.changeStatus(id, productId, status);
        }

        return res;
    }

    private ServiceResponse validate(LinkDTO linkDTO) {
        List<String> problems = new ArrayList<>();

        if (! URLUtils.isAValidURL(linkDTO.getUrl())) {
            problems.add("Invalid URL!");
        } else if (linkDTO.getUrl().length() > 2000) {
            problems.add("The length of URL must be less than 2000 chars!");
        }

        if (linkDTO.getProductId() == null || linkDTO.getProductId() < 1) {
            problems.add("Product cannot be null!");
        } else if (! productRepository.findById(linkDTO.getProductId()).isOK()) {
            problems.add("Unknown product info!");
        }

        return Commons.createResponse(problems);
    }

}
