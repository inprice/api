package io.inprice.scrapper.api.info;

import java.io.Serializable;
import java.util.List;

public class ImportProblem implements Serializable {

    private String info;
    private List<Problem> problems;

    public ImportProblem(String info) {
        this.info = info;
    }

    public ImportProblem(String info, List<Problem> problems) {
        this.info = info;
        this.problems = problems;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public void setProblems(List<Problem> problems) {
        this.problems = problems;
    }
}
