package io.inprice.api.dto;

import java.io.Serializable;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LinkMoveDTO implements Serializable {

	private static final long serialVersionUID = -4093444125338195428L;

	private Long fromProductId;
	private Long toProductId;
	private String toProductName; //in order to create new product if only if toProductId is null!
  private Set<Long> linkIdSet;

}
