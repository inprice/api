package io.inprice.scrapper.api.dto;

public class LinkDTO {

    private Long productId;
    private String url;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
