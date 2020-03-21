package io.inprice.scrapper.api.app.link;

import java.io.Serializable;

import io.inprice.scrapper.api.utils.StringUtils;

public class LinkSpec implements Serializable {

   private static final long serialVersionUID = -7641858030475659639L;

   private Long id;
   private Long linkId;
   private String key;
   private String value;
   private Long companyId;

   public LinkSpec() {
   }

   public LinkSpec(String key, String value) {
      this(null, key, value);
   }

   public LinkSpec(Long linkId, String key, String value) {
      this.linkId = linkId;
      this.key = StringUtils.fixQuotes(key.trim());
      this.value = StringUtils.fixQuotes(value.trim());
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
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
      this.key = StringUtils.fixQuotes(key.trim());
   }

   public String getValue() {
      return value;
   }

   public void setValue(String value) {
      this.value = StringUtils.fixQuotes(value.trim());
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

}
