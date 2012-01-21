package edu.berkeley.sparrow.daemon.scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.THsHaServer.Args;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

import edu.berkeley.sparrow.daemon.SparrowConf;
import edu.berkeley.sparrow.daemon.util.TServerRunnable;
import edu.berkeley.sparrow.thrift.SchedulerService;
import edu.berkeley.sparrow.thrift.TSchedulingRequest;
import edu.berkeley.sparrow.thrift.TTaskPlacement;

/**
 * This class extends the thrift sparrow scheduler interface. It wraps the
 * {@link Scheduler} class and delegates most calls to that class.
 */
public class SchedulerThrift implements SchedulerService.Iface {
  // Defaults if not specified by configuration
  public final static int DEFAULT_SCHEDULER_THRIFT_PORT = 12345;
  private final static int DEFAULT_SCHEDULER_THRIFT_THREADS = 2;

  private final static Logger LOG = Logger.getLogger(SchedulerThrift.class);
  
  private Scheduler scheduler = new Scheduler();

  /**
   * Initialize this thrift service.
   * 
   * This spawns a multi-threaded thrift server and listens for Sparrow
   * scheduler requests.
   */
  public void initialize(Configuration conf) throws TTransportException {
    LOG.setLevel(Level.DEBUG);
    SchedulerService.Processor<SchedulerService.Iface> processor = 
        new SchedulerService.Processor<SchedulerService.Iface>(this);
    scheduler.initialize(conf);
    
    int port = conf.getInt(SparrowConf.SCHEDULER_THRIFT_PORT, 
        DEFAULT_SCHEDULER_THRIFT_PORT);
    TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(
        port);
    
    Args serverArgs = new Args(serverTransport);
    serverArgs.processor(processor);
    
    int threads = conf.getInt(SparrowConf.SCHEDULER_THRIFT_THREADS, 
        DEFAULT_SCHEDULER_THRIFT_THREADS);
    serverArgs.workerThreads(threads);
    TServer server = new THsHaServer(serverArgs);
    LOG.debug("Spawning scheduler server");
    new Thread(new TServerRunnable(server)).start();
  }

  @Override
  public boolean registerFrontend(String app) throws TException {
    return scheduler.registerFrontEnd(app);
  }

  @Override
  public boolean submitJob(TSchedulingRequest req)
      throws TException {
    return scheduler.submitJob(req);
  }

  @Override
  public List<TTaskPlacement> getJobPlacement(TSchedulingRequest req)
      throws TException {
    try {
      return new ArrayList<TTaskPlacement>(scheduler.getJobPlacement(req));

    }
    catch (IOException e) {
      throw new TException(e.getMessage());
    }
  }
}