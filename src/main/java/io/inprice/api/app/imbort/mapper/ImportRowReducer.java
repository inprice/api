package io.inprice.api.app.imbort.mapper;

import java.util.ArrayList;
import java.util.Map;

import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import io.inprice.common.models.Import;
import io.inprice.common.models.ImportDetail;

public class ImportRowReducer implements LinkedHashMapRowReducer<Long, Import> {

  @Override
  public void accumulate(final Map<Long, Import> map, final RowView rowView) {
    final Import imbort = 
      map.computeIfAbsent(rowView.getColumn("i_id", Long.class), id -> rowView.getRow(Import.class));

    try {
      if (imbort.getDetails() == null) imbort.setDetails(new ArrayList<>());
      if (rowView.getColumn("ir_id", Long.class) != null) {
        imbort.getDetails().add(rowView.getRow(ImportDetail.class));
      }
    } catch (Exception e) { }
  }

}
