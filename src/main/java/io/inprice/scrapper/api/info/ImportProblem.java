package io.inprice.scrapper.api.info;

import java.io.Serializable;
import java.util.List;

public class ImportProblem implements Serializable {

	private static final long serialVersionUID = 1L;
	
    private String info;
    private List<String> problems;

    public ImportProblem(String info) {
        this.info = info;
    }

    public ImportProblem(String info, List<String> problems) {
        this.info = info;
        this.problems = problems;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public List<String> getProblems() {
        return problems;
    }

    public void setProblems(List<String> problems) {
        this.problems = problems;
    }
}
