package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.LinkRepository;
import io.inprice.scrapper.api.rest.repository.ProductRepository;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.utils.URLUtils;
import org.eclipse.jetty.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class LinkService {

    private final LinkRepository linkRepository = Beans.getSingleton(LinkRepository.class);
    private final ProductRepository productRepository = Beans.getSingleton(ProductRepository.class);

    public ServiceResponse findById(Long id) {
        return linkRepository.findById(id);
    }

    public ServiceResponse getList(Long productId) {
        if (productId == null || productId < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        return linkRepository.getList(productId);
    }

    public ServiceResponse insert(LinkDTO linkDTO) {
        ServiceResponse res = validate(linkDTO);
        if (res.isOK()) {
            res = linkRepository.insert(linkDTO);
        }
        return res;
    }

    public ServiceResponse deleteById(Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("Link");
        }

        return linkRepository.deleteById(id);
    }

    public ServiceResponse changeStatus(Long id, Long productId, Status status) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("Link");
        }
        if (productId == null || productId < 1) {
            return InstantResponses.NOT_FOUND("Product");
        }

        return linkRepository.changeStatus(id, productId, status);
    }

    private ServiceResponse validate(LinkDTO linkDTO) {
        List<Problem> problems = new ArrayList<>();

        if (! URLUtils.isAValidURL(linkDTO.getUrl())) {
            problems.add(new Problem("url", "Invalid URL!"));
        } else if (linkDTO.getUrl().length() > 2000) {
            problems.add(new Problem("url", "The length of URL must be less than 2000 chars!"));
        }

        if (linkDTO.getProductId() == null || linkDTO.getProductId() < 1) {
            problems.add(new Problem("form", "Product cannot be null!"));
        } else if (! productRepository.findById(linkDTO.getProductId()).isOK()) {
            problems.add(new Problem("form", "Unknown product info!"));
        }

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

}
