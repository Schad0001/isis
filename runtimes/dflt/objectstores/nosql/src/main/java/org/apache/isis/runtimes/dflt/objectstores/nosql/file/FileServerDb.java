/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.runtimes.dflt.objectstores.nosql.file;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.isis.runtimes.dflt.objectstores.nosql.NoSqlCommandContext;
import org.apache.isis.runtimes.dflt.objectstores.nosql.NoSqlDataDatabase;
import org.apache.isis.runtimes.dflt.objectstores.nosql.StateReader;
import org.apache.isis.runtimes.dflt.runtime.persistence.ConcurrencyException;
import org.apache.isis.runtimes.dflt.runtime.persistence.objectstore.transaction.PersistenceCommand;
import org.apache.log4j.Logger;

public class FileServerDb implements NoSqlDataDatabase {

    private static final Logger LOG = Logger.getLogger(FileServerDb.class);

    private final String host;
    private final int port;

    private final int timeout;

    public FileServerDb(final String host, final int port, final int timeout) {
        this.host = host;
        this.port = port == 0 ? 9012 : port;
        this.timeout = timeout;
    }

    // TODO pool connection and reuse
    private ClientConnection getConnection() {
        return new ClientConnection(host, port, timeout);
    }

    // TODO pool connection and reuse
    private void returnConnection(final ClientConnection connection) {
        connection.logComplete();
        connection.close();
    }

    // TODO pool connection and reuse - probably need to replace the connection
    private void abortConnection(final ClientConnection connection) {
        connection.logFailure();
        connection.close();
    }

    @Override
    public StateReader getInstance(final String key, final String specificationName) {
        final ClientConnection connection = getConnection();
        String data;
        try {
            final String request = specificationName + " " + key;
            connection.request('R', request);
            connection.validateRequest();
            data = connection.getResponseData();
        } catch (final RuntimeException e) {
            LOG.error("aborting getInstance", e);
            abortConnection(connection);
            throw e;
        }
        final JsonStateReader reader = new JsonStateReader(data);
        returnConnection(connection);
        return reader;
    }

    @Override
    public Iterator<StateReader> instancesOf(final String specificationName) {
        final ClientConnection connection = getConnection();
        List<StateReader> instances;
        try {
            instances = new ArrayList<StateReader>();
            connection.request('L', specificationName + " 0");
            connection.validateRequest();
            String data;
            while ((data = connection.getResponseData()).length() > 0) {
                final JsonStateReader reader = new JsonStateReader(data);
                instances.add(reader);
            }
        } catch (final RuntimeException e) {
            LOG.error("aborting instancesOf", e);
            abortConnection(connection);
            throw e;
        }
        returnConnection(connection);
        return instances.iterator();

    }

    @Override
    public void write(final List<PersistenceCommand> commands) {
        final ClientConnection connection = getConnection();
        PersistenceCommand currentCommand = null;
        try {
            connection.request('W', "");
            final NoSqlCommandContext context = new FileClientCommandContext(connection);
            for (final PersistenceCommand command : commands) {
                currentCommand = command;
                command.execute(context);
            }
            connection.validateRequest();

        } catch (final ConcurrencyException e) {
            throw e;
        } catch (final RuntimeException e) {
            LOG.error("aborting write, command: " + currentCommand, e);
            abortConnection(connection);
            throw e;
        }
        returnConnection(connection);
    }

    @Override
    public void close() {
    }

    @Override
    public void open() {
    }

    @Override
    public boolean containsData() {
        final ClientConnection connection = getConnection();
        boolean flag;
        try {
            connection.request('X', "contains-data");
            connection.validateRequest();
            flag = connection.getResponseAsBoolean();
        } catch (final RuntimeException e) {
            LOG.error("aborting containsData", e);
            abortConnection(connection);
            throw e;
        }
        returnConnection(connection);
        return flag;
    }

    @Override
    public long nextSerialNumberBatch(final String name, final int batchSize) {
        final ClientConnection connection = getConnection();
        long serialNumber;
        try {
            connection.request('N', name + " " + Integer.toString(batchSize));
            connection.validateRequest();
            serialNumber = connection.getResponseAsLong();
        } catch (final RuntimeException e) {
            LOG.error("aborting nextSerialNumberBatch", e);
            abortConnection(connection);
            throw e;
        }
        returnConnection(connection);
        return serialNumber;
    }

    @Override
    public void addService(final String name, final String key) {
        final ClientConnection connection = getConnection();
        try {
            connection.request('T', name + " " + key);
            connection.validateRequest();
        } catch (final RuntimeException e) {
            LOG.error("aborting addService", e);
            abortConnection(connection);
            throw e;
        }
        returnConnection(connection);
    }

    @Override
    public String getService(final String name) {
        final ClientConnection connection = getConnection();
        String response;
        try {
            connection.request('S', name);
            connection.validateRequest();
            response = connection.getResponse();
        } catch (final RuntimeException e) {
            LOG.error("aborting getServices", e);
            abortConnection(connection);
            throw e;
        }
        returnConnection(connection);
        return response.equals("null") ? null : response;
    }

    @Override
    public boolean hasInstances(final String specificationName) {
        final ClientConnection connection = getConnection();
        boolean hasInstances;
        try {
            connection.request('I', specificationName);
            connection.validateRequest();
            hasInstances = connection.getResponseAsBoolean();
        } catch (final RuntimeException e) {
            LOG.error("aborting hasInstances", e);
            abortConnection(connection);
            throw e;
        }
        returnConnection(connection);
        return hasInstances;
    }
}
