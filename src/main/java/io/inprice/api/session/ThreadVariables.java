package io.inprice.api.session;

import io.inprice.api.session.info.ForRedis;
import io.inprice.common.models.User;

class ThreadVariables {

  private int sessionNo;
  private ForRedis session;
  private User superUser;

  ThreadVariables() { }
  
  //if is a super user
  void set(User superUser) {
  	this.superUser = superUser;
  }

  //if a normal user
  void set(ForRedis session, int sessionNo) {
    this.session = session;
    this.sessionNo = sessionNo;
  }

  ForRedis getSession() {
    return session;
  }

  public int getSessionNo() {
    return sessionNo;
  }
  
  public User getSuperUser() {
		return superUser;
	}

  public Long getWorkspaceId() {
		return superUser.getAccid();
	}

}
