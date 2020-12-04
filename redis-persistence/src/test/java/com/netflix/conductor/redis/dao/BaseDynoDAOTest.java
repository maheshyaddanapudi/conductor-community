/*
 * Copyright 2020 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.redis.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.redis.config.utils.JedisProxy;
import com.netflix.conductor.redis.config.utils.RedisProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseDynoDAOTest {

    @Mock
    private JedisProxy jedisProxy;

    @Mock
    private ObjectMapper objectMapper;

    private RedisProperties properties;

    private BaseDynoDAO baseDynoDAO;

    @Before
    public void setUp() {
        properties = mock(RedisProperties.class);
        this.baseDynoDAO = new BaseDynoDAO(jedisProxy, objectMapper, properties);
    }

    @Test
    public void testNsKey() {
        assertEquals("", baseDynoDAO.nsKey());

        String[] keys = {"key1", "key2"};
        assertEquals("key1.key2", baseDynoDAO.nsKey(keys));

        when(properties.getWorkflowNamespacePrefix()).thenReturn("test");
        assertEquals("test", baseDynoDAO.nsKey());

        assertEquals("test.key1.key2", baseDynoDAO.nsKey(keys));

        when(properties.getStack()).thenReturn("stack");
        assertEquals("test.stack.key1.key2", baseDynoDAO.nsKey(keys));
    }
}
