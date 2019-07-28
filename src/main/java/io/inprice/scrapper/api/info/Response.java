package io.inprice.scrapper.api.info;

import io.inprice.scrapper.common.models.Model;
import org.eclipse.jetty.http.HttpStatus;

import java.util.List;

public class Response<T extends Model> {

    private int status;
    private String result;
    private T model;
    private List<T> modelList;
    private List<Problem> problems;

    public Response(int status) {
        this.status = status;
    }

    public Response(int status, String result) {
        this.status = status;
        this.result = result;
    }

    public Response(T model) {
        this.status = HttpStatus.OK_200;
        this.model = model;
    }

    public boolean isOK() {
        return (status == HttpStatus.OK_200);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(T model) {
        this.model = model;
    }

    public List<T> getModelList() {
        return modelList;
    }

    public void setModelList(List<T> modelList) {
        this.modelList = modelList;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public void setProblems(List<Problem> problems) {
        this.problems = problems;
    }
}
