package io.inprice.scrapper.api.models;

import io.inprice.crawler.common.meta.UserType;
import org.eclipse.jetty.http.HttpStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Customer extends Model {

    private UserType userType;
    private String email;
    private String passwordHash;
    private String passwordSalt;
    private String companyName;
    private String contactName;
    private String website;
    private Long sectorId;
    private Long countryId;
    private Date insertAt;

    //transient fields
    private String password;
    private String token;

    public Customer() {
        setHttpStatus(HttpStatus.OK_200);
    }

    public Customer(int httpStatus, String problem) {
        setHttpStatus(httpStatus);
        setProblems(new ArrayList<>());
        getProblems().add(problem);
    }

    public Customer(int httpStatus, List<String> problems) {
        setHttpStatus(httpStatus);
        setProblems(problems);
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "userType=" + userType +
                ", email='" + email + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", passwordSalt='" + passwordSalt + '\'' +
                ", companyName='" + companyName + '\'' +
                ", contactName='" + contactName + '\'' +
                ", website='" + website + '\'' +
                ", sectorId=" + sectorId +
                ", countryId=" + countryId +
                ", insertAt=" + insertAt +
                '}';
    }
}
