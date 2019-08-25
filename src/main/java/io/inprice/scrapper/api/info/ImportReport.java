package io.inprice.scrapper.api.info;

import java.util.List;

public class ImportReport {

    private int status;
    private String result;

    private int insertCount;
    private int duplicateCount;
    private List<ImportProblem> problems;

    public ImportReport(int status) {
        this.status = status;
    }

    public ImportReport(int status, String result) {
        this.status = status;
        this.result = result;
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

    public int getInsertCount() {
        return insertCount;
    }

    public void setInsertCount(int insertCount) {
        this.insertCount = insertCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public List<ImportProblem> getProblems() {
        return problems;
    }

    public void setProblems(List<ImportProblem> problems) {
        this.problems = problems;
    }
}
