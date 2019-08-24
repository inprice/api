package io.inprice.scrapper.api.info;

import java.io.Serializable;
import java.util.List;

public class ImportProblem implements Serializable {

    private String field;
    private List<Problem> problems;

    public ImportProblem(String field) {
        this.field = field;
    }

    public ImportProblem(String field, List<Problem> problems) {
        this.field = field;
        this.problems = problems;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public void setProblems(List<Problem> problems) {
        this.problems = problems;
    }
}
