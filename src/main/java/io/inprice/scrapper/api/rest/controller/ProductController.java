package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.ProductService;
import io.inprice.scrapper.common.meta.UserType;
import org.apache.commons.validator.routines.LongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private static final ProductService productService = Beans.getSingleton(ProductService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Product.BASE, (req, res) -> {
            ServiceResponse serviceRes = upsert(req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.Product.BASE, (req, res) -> {
            ServiceResponse serviceRes = upsert(req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Product.BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Product.BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Product.BASE + "s", (req, res) -> {
            ServiceResponse serviceRes = getList();
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //toggle active status
        put(Consts.Paths.Product.TOGGLE_STATUS + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = toggleStatus(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse findById(Long id) {
        return productService.findById(id);
    }

    private ServiceResponse getList() {
        return productService.getList();
    }

    private ServiceResponse deleteById(Long id) {
        if (Context.getAuthUser().getType().equals(UserType.USER)) return InstantResponses.PERMISSION_PROBLEM("delete a product!");
        return productService.deleteById(id);
    }

    private ServiceResponse upsert(String body, boolean insert) {
        if (Context.getAuthUser().getType().equals(UserType.USER)) return InstantResponses.PERMISSION_PROBLEM("save a product!");

        ProductDTO productDTO = toModel(body);
        if (productDTO != null) {
            if (insert)
                return productService.insert(productDTO);
            else
                return productService.update(productDTO);
        }
        log.error("Invalid product data: " + body);
        return InstantResponses.INVALID_DATA("product!");
    }

    private ServiceResponse toggleStatus(Long id) {
        if (Context.getAuthUser().getType().equals(UserType.USER)) return InstantResponses.PERMISSION_PROBLEM("toggle a product's status!");
        return productService.toggleStatus(id);
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
