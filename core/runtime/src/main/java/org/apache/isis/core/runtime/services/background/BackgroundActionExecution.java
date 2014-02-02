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
package org.apache.isis.core.runtime.services.background;

import java.util.List;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import org.apache.isis.applib.clock.Clock;
import org.apache.isis.applib.services.background.ActionInvocationMemento;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.applib.services.reifiableaction.ReifiableAction;
import org.apache.isis.applib.services.reifiableaction.ReifiableActionContext;
import org.apache.isis.core.metamodel.adapter.ObjectAdapter;
import org.apache.isis.core.metamodel.adapter.oid.RootOid;
import org.apache.isis.core.metamodel.adapter.oid.RootOidDefault;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.Contributed;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.runtime.services.memento.MementoServiceDefault;
import org.apache.isis.core.runtime.sessiontemplate.AbstractIsisSessionTemplate;

/**
 * Intended to be used as a base class for executing queued up {@link ReifiableAction background action}s.
 * 
 * <p>
 * This implementation uses the {@link #findBackgroundActionsToExecute() hook method} so that it is
 * independent of the location where the actions have actually been persisted to.
 */
public abstract class BackgroundActionExecution extends AbstractIsisSessionTemplate {

    private final MementoServiceDefault mementoService;

    public BackgroundActionExecution() {
        // same as configured by BackgroundServiceDefault
        mementoService = new MementoServiceDefault().withNoEncoding();
    }
    
    // //////////////////////////////////////

    
    protected void doExecute(Object context) {
        final List<? extends ReifiableAction> findBackgroundActionsToExecute = findBackgroundActionsToExecute(); 
        for (final ReifiableAction backgroundAction : findBackgroundActionsToExecute) {
            execute(backgroundAction);
        }
    }

    /**
     * Mandatory hook method
     */
    protected abstract List<? extends ReifiableAction> findBackgroundActionsToExecute();

    // //////////////////////////////////////

    
    private void execute(final ReifiableAction backgroundAction) {
        try {
            reifiableActionContext.setReifiableAction(backgroundAction);

            backgroundAction.setStartedAt(Clock.getTimeAsJavaSqlTimestamp());
            
            final String memento = backgroundAction.getMemento();
            final ActionInvocationMemento aim = new ActionInvocationMemento(mementoService, memento);
            
            final String actionId = aim.getActionId();
   
            final Bookmark targetBookmark = aim.getTarget();
            final Object targetObject = bookmarkService.lookup(targetBookmark);
            
            final ObjectAdapter targetAdapter = adapterFor(targetObject);
            final ObjectSpecification specification = targetAdapter.getSpecification();
   
            final ObjectAction objectAction = findAction(specification, actionId);
            if(objectAction == null) {
                throw new Exception("Unknown action '" + actionId + "'");
            }
            
            final ObjectAdapter[] argAdapters = argAdaptersFor(aim);
            ObjectAdapter resultAdapter = objectAction.execute(targetAdapter, argAdapters);
            if(resultAdapter != null) {
                Bookmark resultBookmark = bookmarkService.bookmarkFor(resultAdapter.getObject());
                backgroundAction.setResult(resultBookmark);
            }

        } catch (Exception e) {
            backgroundAction.setException(Throwables.getStackTraceAsString(e));
        } finally {
            backgroundAction.setCompletedAt(Clock.getTimeAsJavaSqlTimestamp());
        }
    }

    private ObjectAction findAction(final ObjectSpecification specification, final String actionId) {
        final List<ObjectAction> objectActions = specification.getObjectActions(Contributed.INCLUDED);
        for (final ObjectAction objectAction : objectActions) {
            if(objectAction.getIdentifier().toClassAndNameIdentityString().equals(actionId)) {
                return objectAction;
            }
        }
        return null;
    }

    private ObjectAdapter[] argAdaptersFor(final ActionInvocationMemento aim) throws ClassNotFoundException {
        final int numArgs = aim.getNumArgs();
        final List<ObjectAdapter> argumentAdapters = Lists.newArrayList();
        for(int i=0; i<numArgs; i++) {
            final ObjectAdapter argAdapter = argAdapterFor(aim, i);
            argumentAdapters.add(argAdapter);
        }
        return argumentAdapters.toArray(new ObjectAdapter[]{});
    }

    private ObjectAdapter argAdapterFor(final ActionInvocationMemento aim, int num) throws ClassNotFoundException {
        final Class<?> argType = aim.getArgType(num);
        final Object arg = aim.getArg(num, argType);
        if(arg == null) {
            return null;
        }
        if(Bookmark.class != argType) {
            return adapterFor(arg);
        } else {
            final Bookmark argBookmark = (Bookmark)arg;
            final RootOid rootOid = RootOidDefault.create(argBookmark);
            return adapterFor(rootOid);
        }
    }

    
    // //////////////////////////////////////

    @javax.inject.Inject
    private BookmarkService bookmarkService;

    @javax.inject.Inject
    private ReifiableActionContext reifiableActionContext;
}