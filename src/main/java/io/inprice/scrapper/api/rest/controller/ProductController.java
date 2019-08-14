package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.ProductService;
import io.inprice.scrapper.api.rest.service.TokenService;
import org.apache.commons.validator.routines.LongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private static final ProductService productService = Beans.getSingleton(ProductService.class);
    private static final TokenService tokenService = Beans.getSingleton(TokenService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Product.BASE, (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);

            ServiceResponse serviceRes = upsert(authUser, req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.Product.BASE, (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);

            ServiceResponse serviceRes = upsert(authUser, req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Product.BASE + "/:id", (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = deleteById(authUser, id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Product.BASE + "/:id", (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = findById(authUser, id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Product.BASE + "s", (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);

            ServiceResponse serviceRes = getList(authUser);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //toggle active status
        put(Consts.Paths.Product.TOGGLE_STATUS + "/:id", (req, res) -> {
            final AuthUser authUser = tokenService.getAuthUser(req);
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = toggleStatus(authUser, id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse findById(AuthUser authUser, Long id) {
        return productService.findById(authUser, id);
    }

    private ServiceResponse getList(AuthUser authUser) {
        return productService.getList(authUser);
    }

    private ServiceResponse deleteById(AuthUser authUser, Long id) {
        return productService.deleteById(authUser, id);
    }

    private ServiceResponse upsert(AuthUser authUser, String body, boolean insert) {
        ProductDTO productDTO = toModel(body);
        if (productDTO != null) {
            if (insert)
                return productService.insert(authUser, productDTO);
            else
                return productService.update(authUser, productDTO);
        }
        log.error("Invalid product data: " + body);
        return InstantResponses.INVALID_DATA("product!");
    }

    private ServiceResponse toggleStatus(AuthUser authUser, Long id) {
        return productService.toggleStatus(authUser, id);
    }

    private ProductDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, ProductDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for product, body: " + body);
        }

        return null;
    }

}
