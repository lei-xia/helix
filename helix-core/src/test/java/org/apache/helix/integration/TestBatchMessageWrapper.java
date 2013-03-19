package org.apache.helix.integration;

import java.util.Date;

import org.apache.helix.NotificationContext;
import org.apache.helix.PropertyKey.Builder;
import org.apache.helix.TestHelper;
import org.apache.helix.ZNRecord;
import org.apache.helix.ZkUnitTestBase;
import org.apache.helix.manager.zk.ZKHelixDataAccessor;
import org.apache.helix.manager.zk.ZkBaseDataAccessor;
import org.apache.helix.messaging.handling.BatchMessageWrapper;
import org.apache.helix.mock.controller.ClusterController;
import org.apache.helix.mock.participant.MockMSModelFactory;
import org.apache.helix.mock.participant.MockParticipant;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.Message;
import org.apache.helix.tools.ClusterStateVerifier;
import org.apache.helix.tools.ClusterStateVerifier.BestPossAndExtViewZkVerifier;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBatchMessageWrapper extends ZkUnitTestBase {
  class MockBatchMsgWrapper extends BatchMessageWrapper {
    int _startCount = 0;
    int _endCount = 0;
  
    @Override
    public void start(Message batchMsg, NotificationContext context) {
      // System.out.println("test batchMsg.start() invoked, " + batchMsg.getTgtName());
      _startCount++;
    }
    
    @Override
    public void end(Message batchMsg, NotificationContext context) {
      // System.out.println("test batchMsg.end() invoked, " + batchMsg.getTgtName());
      _endCount++;
    }
  }
  
  class TestMockMSModelFactory extends MockMSModelFactory {
    @Override
    public BatchMessageWrapper createBatchMessageWrapper(String resourceName) {
      return new MockBatchMsgWrapper();
    }
  }
  
  @Test
  public void testBasic() throws Exception {
    String className = TestHelper.getTestClassName();
    String methodName = TestHelper.getTestMethodName();
    String clusterName = className + "_" + methodName;
    final int n = 2;
    
    System.out.println("START " + clusterName + " at " + new Date(System.currentTimeMillis()));
    
    TestHelper.setupCluster(clusterName, 
                            ZK_ADDR, 
                            12918, // startPort
                            "localhost", // participantNamePrefix
                            "TestDB", // resourceNamePrefix
                            1, // resourceNb
                            2, // partitionNb
                            n, // nodesNb
                            2, // replica
                            "MasterSlave", 
                            true);
    
    // enable batch-message
    ZKHelixDataAccessor accessor = new ZKHelixDataAccessor(clusterName, 
                                    new ZkBaseDataAccessor<ZNRecord>(_gZkClient));
    Builder keyBuilder = accessor.keyBuilder();
    IdealState idealState = accessor.getProperty(keyBuilder.idealStates("TestDB0"));
    idealState.setBatchMessageMode(true);
    accessor.setProperty(keyBuilder.idealStates("TestDB0"), idealState);
    
    ClusterController controller = new ClusterController(clusterName, "controller_0", ZK_ADDR);
    controller.syncStart();
    
    // start participants
    MockParticipant[] participants = new MockParticipant[n];
    TestMockMSModelFactory[] ftys = new TestMockMSModelFactory[n];
    
    for (int i = 0; i < n; i++) {
      String instanceName = "localhost_" + (12918 + i);
      ftys[i] = new TestMockMSModelFactory();
      participants[i] = new MockParticipant(ftys[i], clusterName, instanceName, ZK_ADDR, null);
      participants[i].syncStart();
      
      // wait for each participant to complete state transitions, so we have deterministic results
      boolean result = false;
      do {
        Thread.sleep(100);
        result = ClusterStateVerifier.verifyByZkCallback(new BestPossAndExtViewZkVerifier(ZK_ADDR, clusterName));
      } while (result == false);
      
      Assert.assertTrue(result, "participant: " + instanceName + " fails to complete all transitions");
    }
    
    // check batch-msg-wrapper counts
    MockBatchMsgWrapper wrapper = (MockBatchMsgWrapper) ftys[0].getBatchMessageWrapper("TestDB0");
    // System.out.println("startCount: " + wrapper._startCount);
    Assert.assertEquals(wrapper._startCount, 3, "Expect 3 batch.start: O->S, S->M, and M->S for 1st participant");
    // System.out.println("endCount: " + wrapper._endCount);
    Assert.assertEquals(wrapper._endCount, 3, "Expect 3 batch.end: O->S, S->M, and M->S for 1st participant");
    
    wrapper = (MockBatchMsgWrapper) ftys[1].getBatchMessageWrapper("TestDB0");
    // System.out.println("startCount: " + wrapper._startCount);
    Assert.assertEquals(wrapper._startCount, 2, "Expect 2 batch.start: O->S and S->M for 2nd participant");
    // System.out.println("endCount: " + wrapper._endCount);
    Assert.assertEquals(wrapper._startCount, 2, "Expect 2 batch.end: O->S and S->M for 2nd participant");
    
    System.out.println("END " + clusterName + " at " + new Date(System.currentTimeMillis()));
  }
  
}