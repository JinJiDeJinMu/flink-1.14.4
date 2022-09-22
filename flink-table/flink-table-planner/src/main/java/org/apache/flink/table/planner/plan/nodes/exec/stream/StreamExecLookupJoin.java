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

package org.apache.flink.table.planner.plan.nodes.exec.stream;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.planner.plan.nodes.exec.InputProperty;
import org.apache.flink.table.planner.plan.nodes.exec.common.CommonExecLookupJoin;
import org.apache.flink.table.planner.plan.nodes.exec.spec.TemporalTableSourceSpec;
import org.apache.flink.table.planner.plan.utils.LookupJoinUtil;
import org.apache.flink.table.runtime.operators.join.FlinkJoinType;
import org.apache.flink.table.types.logical.RowType;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.calcite.rex.RexNode;

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** {@link StreamExecNode} for temporal table join that implemented by lookup. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreamExecLookupJoin extends CommonExecLookupJoin implements StreamExecNode<RowData> {
    public StreamExecLookupJoin(
            FlinkJoinType joinType,
            @Nullable RexNode joinCondition,
            TemporalTableSourceSpec temporalTableSourceSpec,
            Map<Integer, LookupJoinUtil.LookupKey> lookupKeys,
            @Nullable List<RexNode> projectionOnTemporalTable,
            @Nullable RexNode filterOnTemporalTable,
            InputProperty inputProperty,
            RowType outputType,
            String description) {
        this(
                joinType,
                joinCondition,
                temporalTableSourceSpec,
                lookupKeys,
                projectionOnTemporalTable,
                filterOnTemporalTable,
                getNewNodeId(),
                Collections.singletonList(inputProperty),
                outputType,
                description);
    }

    @JsonCreator
    public StreamExecLookupJoin(
            @JsonProperty(FIELD_NAME_JOIN_TYPE) FlinkJoinType joinType,
            @JsonProperty(FIELD_NAME_JOIN_CONDITION) @Nullable RexNode joinCondition,
            @JsonProperty(FIELD_NAME_TEMPORAL_TABLE)
                    TemporalTableSourceSpec temporalTableSourceSpec,
            @JsonProperty(FIELD_NAME_LOOKUP_KEYS) Map<Integer, LookupJoinUtil.LookupKey> lookupKeys,
            @JsonProperty(FIELD_NAME_PROJECTION_ON_TEMPORAL_TABLE) @Nullable
                    List<RexNode> projectionOnTemporalTable,
            @JsonProperty(FIELD_NAME_FILTER_ON_TEMPORAL_TABLE) @Nullable
                    RexNode filterOnTemporalTable,
            @JsonProperty(FIELD_NAME_ID) int id,
            @JsonProperty(FIELD_NAME_INPUT_PROPERTIES) List<InputProperty> inputProperties,
            @JsonProperty(FIELD_NAME_OUTPUT_TYPE) RowType outputType,
            @JsonProperty(FIELD_NAME_DESCRIPTION) String description) {
        super(
                joinType,
                joinCondition,
                temporalTableSourceSpec,
                lookupKeys,
                projectionOnTemporalTable,
                filterOnTemporalTable,
                id,
                inputProperties,
                outputType,
                description);
    }
}
