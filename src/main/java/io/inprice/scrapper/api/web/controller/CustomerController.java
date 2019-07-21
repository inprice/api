package io.inprice.scrapper.api.web.controller;

import io.inprice.scrapper.api.framework.abs.IController;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.framework.Controller;
import io.inprice.scrapper.api.web.service.CustomerService;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Customer;
import org.apache.commons.validator.routines.LongValidator;
import org.eclipse.jetty.http.HttpStatus;

import static spark.Spark.*;

@Controller
public class CustomerController implements IController {

    private static final Logger log = new Logger(CustomerController.class);

    @Override
    public void addRoutes() {
        final String ROOT = "customer";

        get(ROOT + "/by-id/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        get(ROOT + "/by-email/:email", (req, res) -> {
            Response serviceRes = findByEmail(req.params(":email"));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(ROOT, (req, res) -> {
            Response serviceRes = upsert(req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        put(ROOT, (req, res) -> {
            Response serviceRes = upsert(req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);
    }

    private Response findById(Long id) {
        return getService().findById(id);
    }

    private Response findByEmail(String email) {
        return getService().findByEmail(email);
    }

    private Response upsert(String body, boolean insert) {
        Customer customer = toModel(body);
        if (customer != null) {
            if (insert)
                return getService().insert(customer);
            else
                return getService().update(customer);
        }
        log.error("Invalid customer data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid customer data for customer!");
    }

    private Customer toModel(String body) {
        Customer customer = null;
        try {
            customer = Global.gson.fromJson(body, Customer.class);
            if (customer != null) return customer;
        } catch (Exception e) {
            log.error("Data conversion error for customer!", e);
        }

        return null;
    }

    private CustomerService service;

    private CustomerService getService() {
        if (service == null) {
            synchronized (log) {
                if (service == null) {
                    service = new CustomerService();
                }
            }
        }
        return service;
    }

}
