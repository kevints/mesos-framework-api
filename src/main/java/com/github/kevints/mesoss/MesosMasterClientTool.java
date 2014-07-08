package com.github.kevints.mesoss;

import com.github.kevints.jompactor.PID;
import com.github.kevints.mesoss.impl.MesosMasterClientImpl;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

import static com.github.kevints.mesoss.Mesos.FrameworkID;
import static com.github.kevints.mesoss.Mesos.FrameworkInfo;
import static com.github.kevints.mesos.Messages.RegisterFrameworkMessage;

public class MesosMasterClientTool {
  private static final Logger LOG = Logger.getLogger(MesosMasterClientTool.class.getName());

  public static void main(String... args) throws Exception {
    PID masterPid = PID.fromString("master@127.0.0.1:5050");
    LOG.info(masterPid.toString());

    MesosMasterClientImpl client = MesosMasterClientImpl.create(masterPid);
    RegisterFrameworkMessage registerFrameworkMessage = RegisterFrameworkMessage.newBuilder()
        .setFramework(FrameworkInfo.newBuilder()
            .setFailoverTimeout(1000 /* secs */)
            .setId(FrameworkID.newBuilder()
                .setValue("jompactor-1"))
            .setUser("aurora")
            .setName("jompactor"))
        .build();

    Server server = new Server(InetSocketAddress.createUnresolved("127.0.0.1", 8080));

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/scheduler(1)/");

    context.addServlet(new ServletHolder(new SchedulerServlet()), "/scheduler(1)/*");

    client.send(registerFrameworkMessage);
    server.setHandler(context);
    server.start();
    server.join();
  }
}
