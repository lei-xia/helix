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

import org.apache.helix.task.TaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JobMonitor implements JobMonitorMBean {

  private static final String JOB_KEY = "Job";
  private static final Logger LOG = LoggerFactory.getLogger(JobMonitor.class);
  private static final long DEFAULT_RESET_INTERVAL_MS = 60 * 60 * 1000; // 1 hour

  private String _clusterName;
  private String _jobType;

  private long _successfullJobCount;
  private long _failedJobCount;
  private long _abortedJobCount;
  private long _existingJobGauge;
  private long _queuedJobGauge;
  private long _runningJobGauge;
  private long _maximumJobLatencyGauge;
  private long _jobLatencyCount;
  private long _lastResetTime;

  public JobMonitor(String clusterName, String jobType) {
    _clusterName = clusterName;
    _jobType = jobType;
    _successfullJobCount = 0L;
    _failedJobCount = 0L;
    _abortedJobCount = 0L;
    _existingJobGauge = 0L;
    _queuedJobGauge = 0L;
    _runningJobGauge = 0L;
    _lastResetTime = System.currentTimeMillis();
    _jobLatencyCount = 0L;
    _maximumJobLatencyGauge = 0L;
  }

  @Override
  public long getSuccessfulJobCount() {
    return _successfullJobCount;
  }

  @Override
  public long getFailedJobCount() {
    return _failedJobCount;
  }

  @Override
  public long getAbortedJobCount() {
    return _abortedJobCount;
  }

  @Override
  public long getExistingJobGauge() {
    return _existingJobGauge;
  }

  @Override
  public long getQueuedJobGauge() {
    return _queuedJobGauge;
  }

  @Override
  public long getRunningJobGauge() {
    return _runningJobGauge;
  }

  @Override
  public long getMaximumJobLatencyGauge() {
    return _maximumJobLatencyGauge;
  }

  @Override
  public long getJobLatencyCount() {
    return _jobLatencyCount;
  }

  @Override
  public String getSensorName() {
    return String.format("%s.%s.%s", _clusterName, JOB_KEY, _jobType);
  }

  public String getJobType() {
    return _jobType;
  }

  /**
   * Update job counters with transition state
   * @param to The to state of job, cleaned by ZK when it is null
   */

  public void updateJobCounters(TaskState to) {
    updateJobCounters(to, 0);
  }

  public void updateJobCounters(TaskState to, long latency) {
    // TODO maybe use separate TIMED_OUT counter later
    if (to.equals(TaskState.FAILED) || to.equals(TaskState.TIMED_OUT)) {
      _failedJobCount++;
    } else if (to.equals(TaskState.COMPLETED)) {
      _successfullJobCount++;

      // Only count succeeded jobs
      _maximumJobLatencyGauge = Math.max(_maximumJobLatencyGauge, latency);
      _jobLatencyCount += latency > 0 ? latency : 0;
    } else if (to.equals(TaskState.ABORTED)) {
      _abortedJobCount++;
    }


  }

  /**
   * Reset job gauges
   */
  public void resetJobGauge() {
    _queuedJobGauge = 0L;
    _existingJobGauge = 0L;
    _runningJobGauge = 0L;
    if (_lastResetTime + DEFAULT_RESET_INTERVAL_MS < System.currentTimeMillis()) {
      _lastResetTime = System.currentTimeMillis();
      _maximumJobLatencyGauge = 0L;
    }
  }

  /**
   * Refresh job gauges
   * @param to The current state of job
   */
  public void updateJobGauge(TaskState to) {
    _existingJobGauge++;
    if (to == null || to.equals(TaskState.NOT_STARTED)) {
      _queuedJobGauge++;
    } else if (to.equals(TaskState.IN_PROGRESS)) {
      _runningJobGauge++;
    }
  }
}
