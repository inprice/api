package io.inprice.api.app.imbort.mapper;

import java.util.ArrayList;
import java.util.Map;

import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import io.inprice.common.info.ImportRow;
import io.inprice.common.models.Import;

public class ImportRowReducer implements LinkedHashMapRowReducer<Long, Import> {

  @Override
  public void accumulate(final Map<Long, Import> map, final RowView rowView) {
    final Import imbort = 
      map.computeIfAbsent(rowView.getColumn("i_id", Long.class), id -> rowView.getRow(Import.class));

    try {
      if (imbort.getList() == null) imbort.setList(new ArrayList<>());
      if (rowView.getColumn("ir_id", Long.class) != null) {
        imbort.getList().add(rowView.getRow(ImportRow.class));
      }
    } catch (Exception e) { }
  }

}
