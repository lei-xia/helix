package org.apache.helix.common.caches;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.helix.HelixConstants;
import org.apache.helix.HelixDataAccessor;
import org.apache.helix.PropertyKey;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.InstanceConfig;
import org.apache.helix.model.LiveInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache the basic cluster data, including LiveInstances, InstanceConfigs and ExternalViews.
 */
public class BasicClusterDataCache {
  private static Logger LOG = LoggerFactory.getLogger(BasicClusterDataCache.class.getName());

  protected Map<String, LiveInstance> _liveInstanceMap;
  protected Map<String, InstanceConfig> _instanceConfigMap;
  protected Map<String, ExternalView> _externalViewMap;

  protected String _clusterName;

  protected Map<HelixConstants.ChangeType, Boolean> _propertyDataChangedMap;

  public BasicClusterDataCache(String clusterName) {
    _propertyDataChangedMap = new ConcurrentHashMap<>();
    _liveInstanceMap = new HashMap<>();
    _instanceConfigMap = new HashMap<>();
    _externalViewMap = new HashMap<>();
    _clusterName = clusterName;
  }

  /**
   * This refreshes the cluster data by re-fetching the data from zookeeper in an efficient way
   *
   * @param accessor
   *
   * @return
   */
  public synchronized void refresh(HelixDataAccessor accessor) {
    LOG.info("START: BasicClusterDataCache.refresh() for cluster " + _clusterName);
    long startTime = System.currentTimeMillis();
    PropertyKey.Builder keyBuilder = accessor.keyBuilder();

    if (_propertyDataChangedMap.get(HelixConstants.ChangeType.EXTERNAL_VIEW)) {
      long start = System.currentTimeMillis();
      _propertyDataChangedMap.put(HelixConstants.ChangeType.EXTERNAL_VIEW, Boolean.valueOf(false));
      _externalViewMap = accessor.getChildValuesMap(keyBuilder.externalViews());
      LOG.info("Reload ExternalViews: " + _externalViewMap.keySet() + ". Takes " + (
            System.currentTimeMillis() - start) + " ms");
    }

    if (_propertyDataChangedMap.get(HelixConstants.ChangeType.LIVE_INSTANCE)) {
      long start = System.currentTimeMillis();
      _propertyDataChangedMap.put(HelixConstants.ChangeType.LIVE_INSTANCE, Boolean.valueOf(false));
      _liveInstanceMap = accessor.getChildValuesMap(keyBuilder.liveInstances());
      LOG.info("Reload LiveInstances: " + _liveInstanceMap.keySet() + ". Takes " + (
          System.currentTimeMillis() - start) + " ms");
    }

    if (_propertyDataChangedMap.get(HelixConstants.ChangeType.INSTANCE_CONFIG)) {
      long start = System.currentTimeMillis();
      _propertyDataChangedMap
          .put(HelixConstants.ChangeType.INSTANCE_CONFIG, Boolean.valueOf(false));
      _instanceConfigMap = accessor.getChildValuesMap(keyBuilder.instanceConfigs());
      LOG.info("Reload InstanceConfig: " + _instanceConfigMap.keySet() + ". Takes " + (
          System.currentTimeMillis() - start) + " ms");
    }

    long endTime = System.currentTimeMillis();
    LOG.info(
        "END: BasicClusterDataCache.refresh() for cluster " + _clusterName + ", took " + (endTime
            - startTime) + " ms");

    if (LOG.isDebugEnabled()) {
      LOG.debug("LiveInstances: " + _liveInstanceMap);
      LOG.debug("ExternalViews: " + _externalViewMap);
      LOG.debug("InstanceConfigs: " + _instanceConfigMap);
    }
  }

  /**
   * Retrieves the ExternalView for all resources
   *
   * @return
   */
  public Map<String, ExternalView> getExternalViews() {
    return Collections.unmodifiableMap(_externalViewMap);
  }

  /**
   * Returns the LiveInstances for each of the instances that are curretnly up and running
   *
   * @return
   */
  public Map<String, LiveInstance> getLiveInstances() {
    return Collections.unmodifiableMap(_liveInstanceMap);
  }

  /**
   * Returns the instance config map
   *
   * @return
   */
  public Map<String, InstanceConfig> getInstanceConfigMap() {
    return Collections.unmodifiableMap(_instanceConfigMap);
  }

  /**
   * Notify the cache that some part of the cluster data has been changed.
   *
   * @param changeType
   * @param pathChanged
   */
  public void notifyDataChange(HelixConstants.ChangeType changeType, String pathChanged) {
    notifyDataChange(changeType);
  }

  /**
   * Notify the cache that some part of the cluster data has been changed.
   *
   * @param changeType
   */
  public void notifyDataChange(HelixConstants.ChangeType changeType) {
    _propertyDataChangedMap.put(changeType, Boolean.valueOf(true));
  }

  /**
   * Clear the corresponding cache based on change type
   * @param changeType
   */
  public synchronized void clearCache(HelixConstants.ChangeType changeType) {
    switch (changeType) {
    case LIVE_INSTANCE:
      _liveInstanceMap.clear();
      break;
    case INSTANCE_CONFIG:
      _instanceConfigMap.clear();
      break;
    case EXTERNAL_VIEW:
      _externalViewMap.clear();
      break;
    default:
      break;
    }
  }

  /**
   * Indicate that a full read should be done on the next refresh
   */
  public synchronized void requireFullRefresh() {
    for(HelixConstants.ChangeType type : HelixConstants.ChangeType.values()) {
      _propertyDataChangedMap.put(type, Boolean.valueOf(true));
    }
  }

  /**
   * toString method to print the data cache state
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("liveInstaceMap:" + _liveInstanceMap).append("\n");
    sb.append("externalViewMap:" + _externalViewMap).append("\n");
    sb.append("instanceConfigMap:" + _instanceConfigMap).append("\n");

    return sb.toString();
  }
}

