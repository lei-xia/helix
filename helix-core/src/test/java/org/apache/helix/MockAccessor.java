package org.apache.helix;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.I0Itec.zkclient.DataUpdater;
import org.apache.helix.mock.MockBaseDataAccessor;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.model.Message;
import org.apache.helix.model.PauseSignal;
import org.apache.helix.model.StateModelDefinition;
import org.apache.zookeeper.data.Stat;

public class MockAccessor implements HelixDataAccessor {
  private final String _clusterName;
  //Map<String, ZNRecord> data = new HashMap<String, ZNRecord>();
  private final PropertyKey.Builder _propertyKeyBuilder;
  private BaseDataAccessor _baseDataAccessor = new MockBaseDataAccessor();

  public MockAccessor() {
    this("testCluster-" + Math.random() * 10000 % 999);
  }

  public MockAccessor(String clusterName) {
    _clusterName = clusterName;
    _propertyKeyBuilder = new PropertyKey.Builder(_clusterName);
  }

  Map<String, ZNRecord> map = new HashMap<String, ZNRecord>();

  @Override
  public boolean createStateModelDef(StateModelDefinition stateModelDef) {
    return false;
  }

  @Override
  public boolean createControllerMessage(Message message) {
    return false;
  }

  @Override
  public boolean createControllerLeader(LiveInstance leader) {
    return false;
  }

  @Override
  public boolean createPause(PauseSignal pauseSignal) {
    return false;
  }

  @Override public boolean setProperty(PropertyKey key, HelixProperty value) {
    String path = key.getPath();
    _baseDataAccessor.set(path, value.getRecord(), AccessOption.PERSISTENT);
    return true;
  }

  @Override
  public <T extends HelixProperty> boolean updateProperty(PropertyKey key, T value) {
    String path = key.getPath();
    PropertyType type = key.getType();
    if (type.updateOnlyOnExists) {
      if (_baseDataAccessor.exists(path, 0)) {
        if (type.mergeOnUpdate) {
          ZNRecord znRecord = new ZNRecord((ZNRecord) _baseDataAccessor.get(path, null, 0));
          znRecord.merge(value.getRecord());
          _baseDataAccessor.set(path, znRecord, 0);
        } else {
          _baseDataAccessor.set(path, value.getRecord(), 0);
        }
      }
    } else {
      if (type.mergeOnUpdate) {
        if (_baseDataAccessor.exists(path, 0)) {
          ZNRecord znRecord = new ZNRecord((ZNRecord) _baseDataAccessor.get(path, null, 0));
          znRecord.merge(value.getRecord());
          _baseDataAccessor.set(path, znRecord, 0);
        } else {
          _baseDataAccessor.set(path, value.getRecord(), 0);
        }
      } else {
        _baseDataAccessor.set(path, value.getRecord(), 0);
      }
    }

    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends HelixProperty> T getProperty(PropertyKey key) {
    String path = key.getPath();
    Stat stat = new Stat();
    return (T) HelixProperty.convertToTypedInstance(key.getTypeClass(),
        (ZNRecord) _baseDataAccessor.get(path, stat, 0));
  }

  @Override
  public boolean removeProperty(PropertyKey key) {
    String path = key.getPath(); // PropertyPathConfig.getPath(type,
    // _clusterName, keys);
    _baseDataAccessor.remove(path, 0);
    return true;
  }

  @Override
  public List<String> getChildNames(PropertyKey propertyKey) {
    String path = propertyKey.getPath();
    return _baseDataAccessor.getChildNames(path, 0);
  }

  @SuppressWarnings("unchecked")
  @Override public <T extends HelixProperty> List<T> getChildValues(PropertyKey propertyKey) {
    String path = propertyKey.getPath(); // PropertyPathConfig.getPath(type,
    List<ZNRecord> children = _baseDataAccessor.getChildren(path, null, 0);
    return (List<T>) HelixProperty.convertToTypedList(propertyKey.getTypeClass(), children);
  }

  @Override
  public <T extends HelixProperty> Map<String, T> getChildValuesMap(PropertyKey key) {
    List<T> list = getChildValues(key);
    return HelixProperty.convertListToMap(list);
  }

  @Override
  public <T extends HelixProperty> boolean[] createChildren(List<PropertyKey> keys,
      List<T> children) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T extends HelixProperty> boolean[] setChildren(List<PropertyKey> keys, List<T> children) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PropertyKey.Builder keyBuilder() {
    return _propertyKeyBuilder;
  }

  @Override
  public BaseDataAccessor getBaseDataAccessor() {
    return _baseDataAccessor;
  }

  @Override
  public <T extends HelixProperty> boolean[] updateChildren(List<String> paths,
      List<DataUpdater<ZNRecord>> updaters, int options) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T extends HelixProperty> List<T> getProperty(List<PropertyKey> keys) {
    List<T> list = new ArrayList<T>();
    for (PropertyKey key : keys) {
      @SuppressWarnings("unchecked")
      T t = (T) getProperty(key);
      list.add(t);
    }
    return list;
  }
}