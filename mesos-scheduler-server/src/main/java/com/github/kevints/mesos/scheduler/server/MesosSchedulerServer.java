package com.github.kevints.mesos.scheduler.server;

import org.eclipse.jetty.util.component.LifeCycle;

public interface MesosSchedulerServer {
  LifeCycle getServerLifeCycle();

  AuthenticateeServlet getAuthenticateeServlet();
}
