package io.inprice.api.app.product.mapper;

import java.util.ArrayList;
import java.util.Map;

import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import io.inprice.common.models.Product;
import io.inprice.common.models.ProductTag;

public class ProductReducer implements LinkedHashMapRowReducer<Long, Product> {

  @Override
  public void accumulate(final Map<Long, Product> map, final RowView rowView) {
    final Product product = 
      map.computeIfAbsent(rowView.getColumn("p_id", Long.class), id -> rowView.getRow(Product.class));

    try {
      if (product.getTags() == null) product.setTags(new ArrayList<>());
      if (rowView.getColumn("pt_id", Long.class) != null) {
        product.getTags().add(rowView.getRow(ProductTag.class).getName());
      }
    } catch (Exception e) { }
  }

}
