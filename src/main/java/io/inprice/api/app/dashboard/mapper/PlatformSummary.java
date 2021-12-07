package io.inprice.api.app.dashboard.mapper;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformSummary implements Serializable {
  
	private static final long serialVersionUID = 55055308924733337L;

	private String domain;
  private Integer actives;
  private Integer waitings;
  private Integer tryings;
  private Integer problems;
  private Integer total;
  
}
