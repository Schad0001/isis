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
package org.apache.isis.viewer.json.viewer.resources.domaintypes;

import org.apache.isis.core.metamodel.spec.feature.ObjectMember;
import org.apache.isis.viewer.json.applib.JsonRepresentation;
import org.apache.isis.viewer.json.applib.RepresentationType;
import org.apache.isis.viewer.json.applib.links.Rel;
import org.apache.isis.viewer.json.viewer.ResourceContext;
import org.apache.isis.viewer.json.viewer.representations.LinkBuilder;
import org.apache.isis.viewer.json.viewer.representations.LinkFollower;
import org.apache.isis.viewer.json.viewer.representations.ReprRendererAbstract;
import org.apache.isis.viewer.json.viewer.resources.domainobjects.MemberType;

public abstract class AbstractTypeMemberReprRenderer<R extends ReprRendererAbstract<R, ParentSpecAndFeature<T>>, T extends ObjectMember> 
        extends AbstractTypeFeatureReprRenderer<R, T> {

    protected MemberType memberType;

    public AbstractTypeMemberReprRenderer(ResourceContext resourceContext, LinkFollower linkFollower, RepresentationType representationType, JsonRepresentation representation) {
        super(resourceContext, linkFollower, representationType, representation);
    }

    /**
     * null if the feature is an object action param.
     * @return
     */
    public MemberType getMemberType() {
        return memberType;
    }
    
    @Override
    public R with(ParentSpecAndFeature<T> specAndFeature) {
        super.with(specAndFeature);
        memberType = MemberType.determineFrom(objectFeature);
        
        // done eagerly so can use as criteria for x-ro-follow-links
        representation.mapPut(memberType.getJsProp(), objectFeature.getId());
        representation.mapPut("memberType", memberType.getName());

        return cast(this);
    }


    protected void addLinkUpToParent() {
        final LinkBuilder parentLinkBuilder = 
                DomainTypeReprRenderer.newLinkToBuilder(resourceContext, Rel.UP, objectSpecification);
        getLinks().arrayAdd(parentLinkBuilder.build());
    }


    protected void addLinkSelfIfRequired() {
        if(!includesSelf) {
            return;
        } 
        
        final ObjectMember objectMember = (ObjectMember)getObjectFeature();
        final LinkBuilder linkBuilder = LinkBuilder.newBuilder(
                getResourceContext(), Rel.SELF, getRepresentationType(), 
                "domainTypes/%s/%s%s", 
                getParentSpecification().getFullIdentifier(), 
                getMemberType().getUrlPart(), 
                objectMember.getId());
        getLinks().arrayAdd(linkBuilder.build());
    }
    


}