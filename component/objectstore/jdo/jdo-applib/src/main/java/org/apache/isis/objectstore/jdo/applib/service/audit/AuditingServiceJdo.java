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
package org.apache.isis.objectstore.jdo.applib.service.audit;

import java.util.UUID;

import org.apache.isis.applib.AbstractFactoryAndRepository;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.audit.AuditingService3;
import org.apache.isis.applib.services.bookmark.Bookmark;

public class AuditingServiceJdo extends AbstractFactoryAndRepository implements AuditingService3 {

    @Programmatic
    public void audit(
            final UUID transactionId, final Bookmark target, final String propertyId, 
            final String preValue, final String postValue, 
            final String user, final java.sql.Timestamp timestamp) {
        AuditEntryJdo auditEntry = newTransientInstance(AuditEntryJdo.class);
        auditEntry.setTimestamp(timestamp);
        auditEntry.setUser(user);
        auditEntry.setTransactionId(transactionId);
        auditEntry.setTarget(target);
        auditEntry.setPropertyId(propertyId);
        auditEntry.setPreValue(preValue);
        auditEntry.setPostValue(postValue);
        persistIfNotAlready(auditEntry);
    }

}
