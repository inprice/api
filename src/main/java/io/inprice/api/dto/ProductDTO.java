package io.inprice.api.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

  private Long id;
  private String code;
  private String name;
  private String description;
  private BigDecimal price;

  private Long brandId;
  private String brandName;

  private Long categoryId;
  private String categoryName;

  private Long workspaceId;

}
