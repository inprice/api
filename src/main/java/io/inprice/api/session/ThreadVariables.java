package io.inprice.api.session;

import io.inprice.api.session.info.ForRedis;

class ThreadVariables {

  private ForRedis session;

  ThreadVariables() {
  }

  void set(ForRedis session) {
    this.session = session;
  }

  ForRedis getSession() {
    return session;
  }

}
