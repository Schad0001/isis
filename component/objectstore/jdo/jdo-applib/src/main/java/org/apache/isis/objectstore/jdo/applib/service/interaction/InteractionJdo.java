/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.isis.objectstore.jdo.applib.service.interaction;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jdo.annotations.IdentityType;

import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.ActionSemantics;
import org.apache.isis.applib.annotation.ActionSemantics.Of;
import org.apache.isis.applib.annotation.Disabled;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.MemberGroupLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.MultiLine;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Title;
import org.apache.isis.applib.annotation.TypicalLength;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.services.HasTransactionId;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.applib.services.interaction.Interaction;
import org.apache.isis.applib.services.interaction.spi.InteractionService;
import org.apache.isis.applib.util.ObjectContracts;
import org.apache.isis.objectstore.jdo.applib.service.JdoColumnLength;


@javax.jdo.annotations.PersistenceCapable(
        identityType=IdentityType.APPLICATION, 
        table="IsisInteraction")
@javax.jdo.annotations.Queries( {
    @javax.jdo.annotations.Query(
            name="findByTransactionId", language="JDOQL",  
            value="SELECT "
                    + "FROM org.apache.isis.objectstore.jdo.applib.service.interaction.InteractionJdo "
                    + "WHERE transactionId == :transactionId"),
    @javax.jdo.annotations.Query(
            name="findCurrent", language="JDOQL",  
            value="SELECT "
                    + "FROM org.apache.isis.objectstore.jdo.applib.service.interaction.InteractionJdo "
                    + "WHERE completedAt == null "
                    + "ORDER BY timestamp DESC"),
    @javax.jdo.annotations.Query(
            name="findCompleted", language="JDOQL",  
            value="SELECT "
                    + "FROM org.apache.isis.objectstore.jdo.applib.service.interaction.InteractionJdo "
                    + "WHERE completedAt != null "
                    + "ORDER BY timestamp DESC")
})
@ObjectType("IsisInteration")
@MemberGroupLayout(
        columnSpans={6,0,6}, 
        left={"Identifiers","Target","Notes"},
        right={"Detail","Timings","Results"})
@Named("Interaction")
public class InteractionJdo implements Interaction {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(InteractionJdo.class);


    // //////////////////////////////////////
    // transactionId (property)
    // //////////////////////////////////////

        
    private UUID transactionId;

    /**
     * The unique identifier (a GUID) of the transaction in which this interaction occurred.
     */
    @javax.jdo.annotations.PrimaryKey
    @javax.jdo.annotations.Column(allowsNull="false", length=JdoColumnLength.TRANSACTION_ID)
    @TypicalLength(JdoColumnLength.TRANSACTION_ID)
    @MemberOrder(name="Identifiers",sequence = "10")
    @Disabled
    @Override
    public UUID getTransactionId() {
        return transactionId;
    }

    /**
     * <b>NOT API</b>: intended to be called only by the framework.
     * 
     * <p>
     * Implementation notes: copied over from the Isis transaction when the interaction is persisted.
     */
    @Override
    public void setTransactionId(final UUID transactionId) {
        this.transactionId = transactionId;
    }

    // //////////////////////////////////////
    // user (property)
    // //////////////////////////////////////

    private String user;

    @javax.jdo.annotations.Column(allowsNull="false", length=JdoColumnLength.USER_NAME)
    @Title(sequence="2", prepend=", ")
    @MemberOrder(name="Identifiers", sequence = "20")
    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }



    // //////////////////////////////////////
    // timestamp (property)
    // //////////////////////////////////////

    private Timestamp timestamp;

    /**
     * The date/time at which this interaction started.
     */
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="false")
    @MemberOrder(name="Timings", sequence = "30")
    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    
    

    // //////////////////////////////////////
    // targetClass (property)
    // //////////////////////////////////////

    private String targetClass;

    @javax.jdo.annotations.Column(allowsNull="false", length=JdoColumnLength.Interaction.TARGET_CLASS)
    @TypicalLength(30)
    @MemberOrder(name="Target", sequence = "10")
    @Named("Class")
    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(final String targetClass) {
        this.targetClass = Util.abbreviated(targetClass, JdoColumnLength.Interaction.TARGET_CLASS);
    }


    // //////////////////////////////////////
    // targetAction (property)
    // //////////////////////////////////////
    
    private String targetAction;
    
    @javax.jdo.annotations.Column(allowsNull="false", length=JdoColumnLength.Interaction.TARGET_ACTION)
    @TypicalLength(30)
    @MemberOrder(name="Target", sequence = "20")
    @Named("Action")
    public String getTargetAction() {
        return targetAction;
    }
    
    public void setTargetAction(final String targetAction) {
        this.targetAction = Util.abbreviated(targetAction, JdoColumnLength.Interaction.TARGET_ACTION);
    }
    

    // //////////////////////////////////////
    // target (property)
    // openTargetObject (action)
    // //////////////////////////////////////

    @Programmatic
    @Override
    public Bookmark getTarget() {
        return Util.bookmarkFor(getTargetStr());
    }
    
    @Programmatic
    @Override
    public void setTarget(Bookmark target) {
        setTargetStr(Util.asString(target));
    }

    // //////////////////////////////////////
    
    private String targetStr;
    @javax.jdo.annotations.Column(allowsNull="false", length=JdoColumnLength.BOOKMARK, name="target")
    @Named("Target Bookmark")
    @Hidden(where=Where.ALL_TABLES)
    @MemberOrder(name="Target", sequence="3")
    @Disabled
    public String getTargetStr() {
        return targetStr;
    }

    public void setTargetStr(final String targetStr) {
        this.targetStr = targetStr;
    }

    // //////////////////////////////////////

    @ActionSemantics(Of.SAFE)
    @MemberOrder(name="TargetStr", sequence="1")
    @Named("Open")
    public Object openTargetObject() {
        return Util.lookupBookmark(getTarget(), bookmarkService, container);
    }
    public boolean hideOpenTargetObject() {
        return getTarget() == null;
    }

    
    // //////////////////////////////////////
    // actionIdentifier (property)
    // //////////////////////////////////////

    private String actionIdentifier;
    
    @javax.jdo.annotations.Column(allowsNull="false", length=JdoColumnLength.ACTION_IDENTIFIER)
    @Title(sequence="1")
    @TypicalLength(60)
    @Hidden(where=Where.ALL_TABLES)
    @MemberOrder(name="Detail",sequence = "1")
    public String getActionIdentifier() {
        return actionIdentifier;
    }

    public void setActionIdentifier(final String actionIdentifier) {
        this.actionIdentifier = Util.abbreviated(actionIdentifier, JdoColumnLength.ACTION_IDENTIFIER);
    }



    // //////////////////////////////////////
    // arguments (property)
    // //////////////////////////////////////
    
    private String arguments;
    
    @javax.jdo.annotations.Column(allowsNull="false", length=JdoColumnLength.Interaction.ARGUMENTS)
    @MultiLine(numberOfLines=6)
    @Hidden(where=Where.ALL_TABLES)
    @MemberOrder(name="Detail",sequence = "4")
    @Disabled
    public String getArguments() {
        return arguments;
    }
    
    public void setArguments(final String arguments) {
        this.arguments = Util.abbreviated(arguments, JdoColumnLength.Interaction.ARGUMENTS);
    }


    // //////////////////////////////////////
    // startedAt (derived property)
    // //////////////////////////////////////
    
    /**
     * The date/time at which this interaction started.
     */
    @javax.jdo.annotations.NotPersistent
    @MemberOrder(name="Timings", sequence = "3")
    @Disabled
    public Timestamp getStartedAt() {
        return getTimestamp();
    }
    
    
    
    // //////////////////////////////////////
    // completedAt (property)
    // //////////////////////////////////////

    private Timestamp completedAt;

    /**
     * The date/time at which this interaction completed.
     * 
     * <p>
     * Not part of the applib API, because the default implementation is not persistent
     * and so cannot be queried "after the fact".  This JDO-specific class is persistent,
     * and so we can gather this information.
     */
    @javax.jdo.annotations.Persistent
    @javax.jdo.annotations.Column(allowsNull="true")
    @MemberOrder(name="Timings", sequence = "4")
    @Disabled
    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(final Timestamp completed) {
        this.completedAt = completed;
    }


    // //////////////////////////////////////
    // duration (derived property)
    // //////////////////////////////////////

    /**
     * The number of seconds (to 3 decimal places) that this interaction lasted.
     * 
     * <p>
     * Populated only if it has {@link #getCompletedAt() completed}.
     */
    @javax.validation.constraints.Digits(integer=5, fraction=3)
    @Named("Duration")
    @MemberOrder(name="Timings", sequence = "7")
    public BigDecimal getDuration() {
        return Util.durationBetween(getStartedAt(), getCompletedAt());
    }



    // //////////////////////////////////////
    // notes (property)
    // //////////////////////////////////////

    private String notes;

    /**
     * Provides the ability for the end-user to annotate a (potentially long-running)
     * interaction.
     * 
     * <p>
     * Not part of the applib API, because the default implementation is not persistent
     * and so there's no object that can be accessed to be annotated.
     */
    @javax.jdo.annotations.Column(allowsNull="true", length=JdoColumnLength.Interaction.NOTES)
    @MultiLine(numberOfLines=10)
    @Hidden(where=Where.ALL_TABLES)
    @MemberOrder(name="Notes", sequence = "6")
    public String getNotes() {
        return notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }


    // //////////////////////////////////////
    // nature (property)
    // //////////////////////////////////////

    private Nature nature;

    /**
     * Not persisted, used as a hint that an action was invoked explicitly by the user
     * (rather than as a side-effect, eg of rendering an object due to get-after-post).
     */
    @javax.jdo.annotations.NotPersistent
    @Programmatic
    @Override
    public Nature getNature() {
        return nature;
    }
    
    /**
     * <b>NOT API</b>: intended to be called only by the framework.
     * 
     * <p>
     * Implementation notes: populated by the viewer as hint to {@link InteractionService} implementation.
     */
    @Override
    public void setNature(Nature nature) {
        this.nature = nature;
    }

    
    // //////////////////////////////////////
    // result (property)
    // openResultObject (action)
    // //////////////////////////////////////

    @Programmatic
    @Override
    public Bookmark getResult() {
        return Util.bookmarkFor(getResultStr());
    }

    @Programmatic
    @Override
    public void setResult(Bookmark result) {
        setResultStr(Util.asString(result));
    }

    // //////////////////////////////////////
    
    private String resultStr;

    @javax.jdo.annotations.Column(allowsNull="true", length=JdoColumnLength.BOOKMARK, name="result")
    @Hidden(where=Where.ALL_TABLES)
    @Named("Result Bookmark")
    @MemberOrder(name="Results", sequence="25")
    @Disabled
    public String getResultStr() {
        return resultStr;
    }

    public void setResultStr(final String resultStr) {
        this.resultStr = resultStr;
    }

    // //////////////////////////////////////

    @ActionSemantics(Of.SAFE)
    @MemberOrder(name="ResultStr", sequence="1")
    @Named("Open")
    public Object openResultObject() {
        return Util.lookupBookmark(getResult(), bookmarkService, container);
    }
    public boolean hideOpenResultObject() {
        return getResult() == null;
    }


    // //////////////////////////////////////
    // exception (property)
    // causedException (derived property)
    // showException (associated action)
    // //////////////////////////////////////

    private String exception;

    /**
     * Stack trace of any exception that might have occurred if this interaction/transaction aborted.
     * 
     * <p>
     * Not visible in the UI, but accessible 
     * <p>
     * Not part of the applib API, because the default implementation is not persistent
     * and so there's no object that can be accessed to be annotated.
     */
    @javax.jdo.annotations.Column(allowsNull="true", length=JdoColumnLength.Interaction.EXCEPTION)
    @Hidden
    @Override
    public String getException() {
        return exception;
    }

    @Override
    public void setException(final String exception) {
        this.exception = Util.abbreviated(exception, JdoColumnLength.Interaction.EXCEPTION);
    }
    
    
    // //////////////////////////////////////
    
    @javax.jdo.annotations.NotPersistent
    @MemberOrder(name="Results",sequence = "30")
    @Hidden(where=Where.ALL_TABLES)
    public boolean isCausedException() {
        return getException() != null;
    }

    
    // //////////////////////////////////////
    
    @ActionSemantics(Of.SAFE)
    @MemberOrder(name="causedException", sequence = "1")
    public String showException() {
        return getException();
    }
    public boolean hideShowException() {
        return !isCausedException();
    }


    // //////////////////////////////////////
    // next(...) impl
    // //////////////////////////////////////

    private final Map<String, AtomicInteger> sequenceByName = Maps.newHashMap();

    @Programmatic
    @Override
    public int next(String sequenceName) {
        AtomicInteger next = sequenceByName.get(sequenceName);
        if(next == null) {
            next = new AtomicInteger(0);
            sequenceByName.put(sequenceName, next);
        } else {
            next.incrementAndGet();
        }
        return next.get();
    }

    // //////////////////////////////////////
    // toString
    // //////////////////////////////////////


    @Override
    public String toString() {
        return ObjectContracts.toString(this, "startedAt,user,actionIdentifier,target,completedAt,duration,transactionId");
    }

    
    // //////////////////////////////////////
    
    @javax.inject.Inject
    private BookmarkService bookmarkService;
    
    @javax.inject.Inject
    private DomainObjectContainer container;
}
