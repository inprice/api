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

	private Long fromGroupId;
	private Long toGroupId;
	private String toGroupName; //in order to create new group if only if toGroupId is null!
  private Set<Long> linkIdSet;

}