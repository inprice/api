package io.inprice.scrapper.api.info;

import java.io.Serializable;

public class Problem implements Serializable {

    private String field;
    private String reason;

    public Problem(String field, String reason) {
        this.field = field;
        this.reason = reason;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "Field=" + field + ", Reason=" + reason;
    }
}
