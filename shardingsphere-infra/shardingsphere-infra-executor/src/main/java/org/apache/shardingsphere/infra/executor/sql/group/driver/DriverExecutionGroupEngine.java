/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.executor.sql.group.driver;

import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroup;
import org.apache.shardingsphere.infra.executor.sql.ConnectionMode;
import org.apache.shardingsphere.infra.executor.sql.context.ExecutionUnit;
import org.apache.shardingsphere.infra.executor.sql.context.SQLUnit;
import org.apache.shardingsphere.infra.executor.sql.group.AbstractExecutionGroupEngine;
import org.apache.shardingsphere.infra.executor.sql.execute.resourced.ExecutionConnection;
import org.apache.shardingsphere.infra.executor.sql.execute.resourced.ResourceManagedExecuteUnit;
import org.apache.shardingsphere.infra.executor.sql.execute.resourced.StorageResourceOption;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Driver execution group engine.
 * 
 * @param <T> type of storage resource execute unit
 * @param <E> type of execution connection
 * @param <C> type of resource connection
 * @param <O> type of storage resource option
 */
public abstract class DriverExecutionGroupEngine
        <T extends ResourceManagedExecuteUnit<?>, E extends ExecutionConnection<C, ?, O>, C, O extends StorageResourceOption> extends AbstractExecutionGroupEngine<T> {
    
    private final E executionConnection;
    
    private final O option;
    
    protected DriverExecutionGroupEngine(final int maxConnectionsSizePerQuery, final E executionConnection, final O option, final Collection<ShardingSphereRule> rules) {
        super(maxConnectionsSizePerQuery, rules);
        this.executionConnection = executionConnection;
        this.option = option;
    }
    
    @Override
    protected final List<ExecutionGroup<T>> group(final String dataSourceName, final List<List<SQLUnit>> sqlUnitGroups, final ConnectionMode connectionMode) throws SQLException {
        List<ExecutionGroup<T>> result = new LinkedList<>();
        List<C> connections = executionConnection.getConnections(dataSourceName, sqlUnitGroups.size(), connectionMode);
        int count = 0;
        for (List<SQLUnit> each : sqlUnitGroups) {
            result.add(createExecutionGroup(dataSourceName, each, connections.get(count++), connectionMode));
        }
        return result;
    }
    
    private ExecutionGroup<T> createExecutionGroup(final String dataSourceName, final List<SQLUnit> sqlUnits, final C connection, final ConnectionMode connectionMode) throws SQLException {
        List<T> result = new LinkedList<>();
        for (SQLUnit each : sqlUnits) {
            result.add(createStorageResourceExecuteUnit(new ExecutionUnit(dataSourceName, each), executionConnection, connection, connectionMode, option));
        }
        return new ExecutionGroup<>(result);
    }
    
    protected abstract T createStorageResourceExecuteUnit(ExecutionUnit executionUnit, E executionConnection, C connection, ConnectionMode connectionMode, O option) throws SQLException;
}
