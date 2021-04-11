package io.inprice.api.dto;

import java.io.Serializable;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LinkDeleteDTO implements Serializable {

	private static final long serialVersionUID = 8504119602766630634L;

	private Long fromGroupId;
  private Set<Long> linkIdSet;

}
