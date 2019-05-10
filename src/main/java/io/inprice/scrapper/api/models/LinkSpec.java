package io.inprice.scrapper.api.models;

import java.math.BigDecimal;
import java.util.Date;

public class LinkSpec extends Model {

    private Long linkId;
    private String key;
    private String value;

    public LinkSpec() {
    }

    public LinkSpec(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public LinkSpec(Long linkId, String key, String value) {
        this.linkId = linkId;
        this.key = key;
        this.value = value;
    }

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
