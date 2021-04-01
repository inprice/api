package io.inprice.api.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LinkMoveDTO implements Serializable {

	private static final long serialVersionUID = -4093444125338195428L;

	private Long toGroupId;
  private List<Long> linkIdList;

}
