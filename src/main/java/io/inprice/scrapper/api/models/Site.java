package io.inprice.scrapper.api.models;

import java.util.Date;

public class Site extends Model {

    private Boolean active;
    private String name;
    private String url;
    private String currencyCode;
    private String currencySymbol;
    private String thousandSeparator;
    private String decimalSeparator;
    private String logo;
    private String logoMini;
    private String domain;
    private String className;
    private Long countryId;
    private Date insertAt;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public String getThousandSeparator() {
        return thousandSeparator;
    }

    public void setThousandSeparator(String thousandSeparator) {
        this.thousandSeparator = thousandSeparator;
    }

    public String getDecimalSeparator() {
        return decimalSeparator;
    }

    public void setDecimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getLogoMini() {
        return logoMini;
    }

    public void setLogoMini(String logoMini) {
        this.logoMini = logoMini;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Long getCountryId() {
        return countryId;
    }

    public void setCountryId(Long countryId) {
        this.countryId = countryId;
    }

    public Date getInsertAt() {
        return insertAt;
    }

    public void setInsertAt(Date insertAt) {
        this.insertAt = insertAt;
    }

    @Override
    public String toString() {
        return "Site{" +
                "active=" + active +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                ", currencySymbol='" + currencySymbol + '\'' +
                ", thousandSeparator='" + thousandSeparator + '\'' +
                ", decimalSeparator='" + decimalSeparator + '\'' +
                ", logo='" + logo + '\'' +
                ", logoMini='" + logoMini + '\'' +
                ", domain='" + domain + '\'' +
                ", className='" + className + '\'' +
                ", countryId=" + countryId +
                ", insertAt=" + insertAt +
                '}';
    }
}
