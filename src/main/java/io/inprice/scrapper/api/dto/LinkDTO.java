package io.inprice.scrapper.api.dto;

public class LinkDTO {

    private Long productId;
    private String url;

    public LinkDTO() {
    }

    public LinkDTO(Long productId) {
        this.productId = productId;
    }

    public LinkDTO(Long productId, String url) {
        this.productId = productId;
        this.url = url;
    }

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
