package io.inprice.scrapper.api.dto;

import java.io.Serializable;

/**
 * Used for handling company info in client side
 */
public class CompanyDTO extends UserDTO implements Serializable {

    //company
    private String companyName;
    private String website;
    private Long countryId;

    //security
    private String captcha;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Long getCountryId() {
        return countryId;
    }

    public void setCountryId(Long countryId) {
        this.countryId = countryId;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }
}
