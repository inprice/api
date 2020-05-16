package io.inprice.scrapper.api.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProductDTO {

    private Long id;
    private String code;
    private String name;
    private String brand;
    private String category;
    private BigDecimal price;
    private Long importId;

}
