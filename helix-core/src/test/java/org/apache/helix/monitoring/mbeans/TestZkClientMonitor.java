package org.apache.helix.monitoring.mbeans;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestZkClientMonitor {

  private MBeanServer _beanServer = ManagementFactory.getPlatformMBeanServer();

  private ObjectName buildObjectName(String tag) throws MalformedObjectNameException {
    return MBeanRegistrar.buildObjectName(ZkClientMonitor.DOMAIN, ZkClientMonitor.TAG, tag);
  }

  private ObjectName buildObjectName(String tag, int num) throws MalformedObjectNameException {
    return MBeanRegistrar.buildObjectName(num, ZkClientMonitor.DOMAIN, ZkClientMonitor.TAG, tag);
  }

  @Test public void testMBeanRegisteration() throws JMException {
    final String TEST_TAG_1 = "test_tag_1";

    ZkClientMonitor monitor = new ZkClientMonitor(TEST_TAG_1);
    Assert.assertTrue(_beanServer.isRegistered(buildObjectName(TEST_TAG_1)));
    ZkClientMonitor monitorDuplicate = new ZkClientMonitor(TEST_TAG_1);
    Assert.assertTrue(_beanServer.isRegistered(buildObjectName(TEST_TAG_1, 1)));

    monitor.unregister();
    monitorDuplicate.unregister();

    Assert.assertFalse(_beanServer.isRegistered(buildObjectName(TEST_TAG_1)));
    Assert.assertFalse(_beanServer.isRegistered(buildObjectName(TEST_TAG_1, 1)));
  }

  @Test public void testCounter() throws JMException {
    final String TEST_TAG = "test_tag_3";
    ZkClientMonitor monitor = new ZkClientMonitor(TEST_TAG);
    ObjectName name = buildObjectName(TEST_TAG);

    monitor.increaseDataChangeEventCounter();
    long eventCount = (long) _beanServer.getAttribute(name, "DataChangeEventCounter");
    Assert.assertEquals(eventCount, 1);

    monitor.increaseReadCounters("TEST/IDEALSTATES/myResource");
    Assert.assertEquals((long) _beanServer.getAttribute(name, "ReadCounter"), 1);
    Assert.assertEquals((long) _beanServer.getAttribute(name, "IdealStatesReadCounter"), 1);

    monitor.increaseWriteCounters("TEST/INSTANCES/node_1/CURRENTSTATES/session_1/Resource", 5);
    Assert.assertEquals((long) _beanServer.getAttribute(name, "WriteCounter"), 1);
    Assert.assertEquals((long) _beanServer.getAttribute(name, "CurrentStatesWriteCounter"), 1);
    Assert.assertEquals((long) _beanServer.getAttribute(name, "CurrentStatesWriteBytesCounter"), 5);
    Assert.assertEquals((long) _beanServer.getAttribute(name, "InstancesWriteCounter"), 1);
    Assert.assertEquals((long) _beanServer.getAttribute(name, "InstancesWriteBytesCounter"), 5);
  }
}