/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.leaderelection;

/**
 * LeaderElectionDriver 负责执行领导者选举并存储领导者信息。所有的领导者内部状态都由 LeaderElectionService 中的锁来保护。
 * 不同的驱动实现不需要关心锁。如果它想要响应领导者变更事件，它应该使用 LeaderElectionEventHandler。 重要提示：LeaderElectionDriver 不能保证在
 * close() 之后没有发生 LeaderElectionEventHandler 回调。
 */

/**
 * A {@link LeaderElectionDriver} is responsible for performing the leader election and storing the
 * leader information. All the leader internal state is guarded by lock in {@link
 * LeaderElectionService}. Different driver implementations do not need to care about the lock. And
 * it should use {@link LeaderElectionEventHandler} if it want to respond to the leader change
 * events.
 *
 * <p><strong>Important</strong>: The {@link LeaderElectionDriver} could not guarantee that there is
 * no {@link LeaderElectionEventHandler} callbacks happen after {@link #close()}.
 */
public interface LeaderElectionDriver {

    /**
     * Write the current leader information to external persistent storage(e.g. Zookeeper,
     * Kubernetes ConfigMap). This is a blocking IO operation. The write operation takes effect only
     * when the driver still has the leadership.
     *
     * @param leaderInformation current leader information. It could be {@link
     *     LeaderInformation#empty()}, which means the caller want to clear the leader information
     *     on external storage. Please remember that the clear operation should only happen before a
     *     new leader is elected and has written his leader information on the storage. Otherwise,
     *     we may have a risk to wrongly update the storage with empty leader information.
     */
    void writeLeaderInformation(LeaderInformation leaderInformation);

    /**
     * Check whether the driver still have the leadership in the distributed coordination system.
     *
     * @return Return whether the driver has leadership.
     */
    boolean hasLeadership();

    /** Close the services used for leader election. */
    void close() throws Exception;
}
