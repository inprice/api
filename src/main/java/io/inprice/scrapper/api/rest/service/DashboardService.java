package io.inprice.scrapper.api.rest.service;

import com.google.gson.JsonObject;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.rest.repository.DashboardRepository;

public class DashboardService {

    private final DashboardRepository repository = Beans.getSingleton(DashboardRepository.class);

    public JsonObject getReport() {
        //TODO: a caching mechanism with TTL must be added here
        return repository.getReport();
    }

}
