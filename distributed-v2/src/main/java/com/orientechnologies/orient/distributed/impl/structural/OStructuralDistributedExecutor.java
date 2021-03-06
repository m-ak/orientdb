package com.orientechnologies.orient.distributed.impl.structural;

import com.orientechnologies.orient.core.db.config.ONodeIdentity;
import com.orientechnologies.orient.distributed.OrientDBDistributed;
import com.orientechnologies.orient.distributed.impl.coordinator.OLogId;
import com.orientechnologies.orient.distributed.impl.coordinator.OLogRequest;
import com.orientechnologies.orient.distributed.impl.coordinator.OOperationLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class OStructuralDistributedExecutor implements AutoCloseable, OOperationContext {

  private       OOperationLog                                    operationLog;
  private       ExecutorService                                  executor;
  private       OrientDBDistributed                              orientDB;
  private final Map<ONodeIdentity, OStructuralDistributedMember> members = new ConcurrentHashMap<>();

  public OStructuralDistributedExecutor(ExecutorService executor, OOperationLog operationLog, OrientDBDistributed orientDB) {
    this.operationLog = operationLog;
    this.executor = executor;
    this.orientDB = orientDB;
  }

  public void receive(OStructuralDistributedMember member, OLogId opId, OStructuralNodeRequest request) {
    //TODO: sort for opId before execute and execute the operations only if is strictly sequential, otherwise wait.
    executor.execute(() -> {
      operationLog.logReceived(opId, request);
      OStructuralNodeResponse response;
      response = request.execute(this);
      member.sendResponse(opId, response);
    });
  }

  @Override
  public void close() {
    executor.shutdown();
    try {
      executor.awaitTermination(1, TimeUnit.HOURS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public void join(OStructuralDistributedMember member) {
    members.put(member.getIdentity(), member);
  }

  public OStructuralDistributedMember getMember(ONodeIdentity senderNode) {
    return members.get(senderNode);
  }

  public void leave(OStructuralDistributedMember member) {
    members.remove(member.getIdentity());
  }

  @Override
  public OrientDBDistributed getOrientDB() {
    return orientDB;
  }

  public void recover(OLogId logId, OStructuralNodeRequest request) {
    executor.execute(() -> {
      request.recover(this);
    });

  }
}
