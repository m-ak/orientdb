/*
 * Copyright 2010-2013 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.server.distributed;

import com.orientechnologies.common.util.OCallable;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.Assert;
import org.junit.Test;

/**
 * Distributed test with 2 servers (0 and 1) running as dynamic and after a while the server 1 is isolated from the network (using a
 * proxy) and then it re-merges the cluster again.
 */
public class SplitBrainNetwork2DynamicServersTest extends AbstractHARemoveNode {
  private final static int SERVERS = 2;

  @Test
  public void test() throws Exception {
    useTransactions = true;
    count = 10;
    startupNodesInSequence = true;
    init(SERVERS);
    prepare(false);
    execute();
  }

  @Override
  protected void onAfterExecution() throws Exception {
    Assert.assertEquals("europe-0", serverInstance.get(0).getServerInstance().getDistributedManager().getLockManagerServer());
    Assert.assertEquals("europe-0", serverInstance.get(1).getServerInstance().getDistributedManager().getLockManagerServer());

    checkInsertedEntries(executeTestsOnServers);
    checkIndexedEntries(executeTestsOnServers);

    serverInstance.get(1).disconnectFrom(serverInstance.get(0));

    banner("SERVER " + (SERVERS - 1) + " HAS BEEN ISOLATED, WAITING FOR THE DATABASE ON SERVER " + (SERVERS - 1)
        + " TO BE OFFLINE...");

    // CHECK THE SPLIT
    waitForDatabaseStatus(0, "europe-1", getDatabaseName(), ODistributedServerManager.DB_STATUS.NOT_AVAILABLE, 30000);
    assertDatabaseStatusEquals(0, "europe-1", getDatabaseName(), ODistributedServerManager.DB_STATUS.NOT_AVAILABLE);
    assertDatabaseStatusEquals(0, "europe-0", getDatabaseName(), ODistributedServerManager.DB_STATUS.ONLINE);

    waitForDatabaseStatus(1, "europe-0", getDatabaseName(), ODistributedServerManager.DB_STATUS.NOT_AVAILABLE, 90000);
    assertDatabaseStatusEquals(1, "europe-0", getDatabaseName(), ODistributedServerManager.DB_STATUS.NOT_AVAILABLE);
    assertDatabaseStatusEquals(1, "europe-1", getDatabaseName(), ODistributedServerManager.DB_STATUS.ONLINE);

    Assert.assertEquals("europe-0", serverInstance.get(0).getServerInstance().getDistributedManager().getLockManagerServer());
    Assert.assertEquals("europe-1", serverInstance.get(1).getServerInstance().getDistributedManager().getLockManagerServer());

    banner("RUN TEST WITHOUT THE OFFLINE SERVER " + (SERVERS - 1) + "...");

    checkInsertedEntries(executeTestsOnServers);
    checkIndexedEntries(executeTestsOnServers);

    count = 10;

    // ONLY ONE SERVER SHOULD HAVE THE RECORDS
    final long currentRecords = expected;

    executeTestsOnServers = createServerList(0);
    try {
      executeMultipleTest();
    } catch (AssertionError e) {
      final String message = e.getMessage();
      Assert.assertTrue(message, message
          .contains("Server 1 count is not what was expected expected:<" + expected + "> but was:<" + (currentRecords) + ">"));
    }

    banner("TEST WITH THE ISOLATED NODE FINISHED, REJOIN THE SERVER " + (SERVERS - 1) + "...");

    // FORCE THE REJOIN
    serverInstance.get(1).rejoin(serverInstance.get(0));

    count = 1000;
    // CREATE NEW RECORD IN THE MEANWHILE ON SERVERS 1 AND 2
    banner("RUNNING ONE WRITER ONLY ON SERVER 0");
    createWriter(0, 100, getDatabaseURL(serverInstance.get(0))).call();

    expected += count;

    count = 10;

    waitForDatabaseIsOnline(0, "europe-0", getDatabaseName(), 90000);
    waitForDatabaseIsOnline(0, "europe-1", getDatabaseName(), 30000);
    assertDatabaseStatusEquals(0, "europe-1", getDatabaseName(), ODistributedServerManager.DB_STATUS.ONLINE);

    waitForDatabaseIsOnline(1, "europe-0", getDatabaseName(), 30000);
    waitForDatabaseIsOnline(1, "europe-1", getDatabaseName(), 30000);
    assertDatabaseStatusEquals(1, "europe-0", getDatabaseName(), ODistributedServerManager.DB_STATUS.ONLINE);

    Assert.assertEquals("europe-0", serverInstance.get(0).getServerInstance().getDistributedManager().getLockManagerServer());
    Assert.assertEquals("europe-0", serverInstance.get(1).getServerInstance().getDistributedManager().getLockManagerServer());

    banner("NETWORK FOR THE ISOLATED NODE " + (SERVERS - 1) + " HAS BEEN RESTORED");

    waitFor(0, new OCallable<Boolean, ODatabaseDocumentTx>() {
      @Override
      public Boolean call(final ODatabaseDocumentTx db) {
        return db.countClass("Person") == expected;
      }
    }, 10000);

    waitFor(1, new OCallable<Boolean, ODatabaseDocumentTx>() {
      @Override
      public Boolean call(final ODatabaseDocumentTx db) {
        final long total = db.countClass("Person");
        if (total != expected)
          System.out.println("Waiting for record count reaching " + expected + ", now it's " + total);

        return total == expected;
      }
    }, 30000);

    poolFactory.reset();

    checkInsertedEntries(executeTestsOnServers);
    checkIndexedEntries(executeTestsOnServers);

    banner("RESTARTING TESTS WITH SERVER " + (SERVERS - 1) + " CONNECTED...");

    count = 10;

    executeMultipleTest();
  }

  protected String getDatabaseURL(final ServerRun server) {
    return "plocal:" + server.getDatabasePath(getDatabaseName());
  }

  @Override
  protected String getDistributedServerConfiguration(final ServerRun server) {
    return "proxied-dyn-orientdb-dserver-config-" + server.getServerId() + ".xml";
  }

  @Override
  public String getDatabaseName() {
    return "ha-split-2-dynamic";
  }
}
