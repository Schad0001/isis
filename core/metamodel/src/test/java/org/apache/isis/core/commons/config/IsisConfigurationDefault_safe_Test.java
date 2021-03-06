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
package org.apache.isis.core.commons.config;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IsisConfigurationDefault_safe_Test {

    @Test
    public void not_a_password() throws Exception {
        assertThat(IsisConfigurationDefault.safe("foo", "bar"), is(equalTo("bar")));
    }

    @Test
    public void a_password() throws Exception {
        assertThat(IsisConfigurationDefault.safe("xyz.password.abc", "bar"), is(equalTo("*******")));
    }

    @Test
    public void a_PassWord() throws Exception {
        assertThat(IsisConfigurationDefault.safe("xyz.PassWord.abc", "bar"), is(equalTo("*******")));
    }

    @Test
    public void is_null() throws Exception {
        assertThat(IsisConfigurationDefault.safe(null, "bar"), is(equalTo("bar")));
    }

}