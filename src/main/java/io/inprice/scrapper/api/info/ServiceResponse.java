package io.inprice.scrapper.api.info;

import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.common.models.Model;

import java.util.List;

public final class ServiceResponse<T extends Model> {

    private int status;
    private T model;
    private List<T> models;
    private List<Problem> problems;

    private String data;

    public ServiceResponse(int status) {
        this.status = status;
    }

    public ServiceResponse(T model) {
        this.status = Responses.OK.getStatus();
        this.model = model;
    }

    public ServiceResponse(List<T> models) {
        this.status = Responses.OK.getStatus();
        this.models = models;
    }

    public ServiceResponse(String data) {
        this.status = Responses.OK.getStatus();
        this.data = data;
    }

    public boolean isOK() {
        return (status == Responses.OK.getStatus());
    }

    public int getStatus() {
        return status;
    }

    public T getModel() {
        return model;
    }

    public List<T> getModels() {
        return models;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public String getData() {
        return data;
    }

    public static ServiceResponse create(List<Problem> problems) {
        ServiceResponse res = new ServiceResponse(Responses.DataProblem.FORM_VALIDATION.getStatus());
        res.problems = problems;
        return res;
    }
}
