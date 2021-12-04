package io.inprice.api.session.info;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ForDatabase implements Serializable {

  private static final long serialVersionUID = 5073154009606337304L;

  private String hash;
  private Long userId;
  private Long workspaceId;
  private String ip;
  private String userAgent;
  private Date accessedAt = new Date();

  //transient
  private String workspaceName;

}