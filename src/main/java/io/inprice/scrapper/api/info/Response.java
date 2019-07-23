package io.inprice.scrapper.api.info;

import io.inprice.scrapper.common.models.Model;
import org.eclipse.jetty.http.HttpStatus;

import java.util.List;

public class Response {

    private int status;
    private String result;
    private Model model;
    private List<Model> modelList;
    private List<String> problemList;

    public Response(int status) {
        this.status = status;
    }

    public Response(int status, String result) {
        this.status = status;
        this.result = result;
    }

    public Response(Model model) {
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

    public void setModel(Model model) {
        this.model = model;
    }

    public List<Model> getModelList() {
        return modelList;
    }

    public void setModelList(List<Model> modelList) {
        this.modelList = modelList;
    }

    public List<String> getProblemList() {
        return problemList;
    }

    public void setProblemList(List<String> problemList) {
        this.problemList = problemList;
    }
}
