package io.inprice.scrapper.api.models;

import java.io.Serializable;
import java.util.List;

public class Model implements Serializable {

    private Long id;

    private int httpStatus;
    private List<String> problems;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public List<String> getProblems() {
        return problems;
    }

    public void setProblems(List<String> problems) {
        this.problems = problems;
    }
}
