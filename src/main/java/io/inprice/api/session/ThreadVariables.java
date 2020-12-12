package io.inprice.api.session;

import io.inprice.api.session.info.ForRedis;

class ThreadVariables {

  private int sessionNo;
  private ForRedis session;

  ThreadVariables() {
  }

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

}
