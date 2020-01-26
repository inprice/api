package io.inprice.scrapper.api.dto;

/**
 * Used for handling company info from client side
 */
public class CompanyDTO extends UserDTO {

	private static final long serialVersionUID = 1L;
	
    //company
    private String companyName;
    private String website;
    private String country;
    private String sector;

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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

}
