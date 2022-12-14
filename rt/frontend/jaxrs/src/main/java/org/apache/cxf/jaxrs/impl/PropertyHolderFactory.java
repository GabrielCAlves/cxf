/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;

public final class PropertyHolderFactory {
    private PropertyHolderFactory() {

    }

    public static PropertyHolder getPropertyHolder(Message m) {
        return m.containsKey("HTTP.REQUEST") ? new ServletRequestPropertyHolder(m) : new MessagePropertyHolder(m);
    }

    public interface PropertyHolder {
        Object getProperty(String name);
        void removeProperty(String name);
        void setProperty(String name, Object value);
        Collection<String> getPropertyNames();
    }
    
    private static class ServletRequestPropertyHolder extends MessagePropertyHolder {
        private static final String ENDPOINT_ADDRESS_PROPERTY = "org.apache.cxf.transport.endpoint.address";
        private final HttpServletRequest request;
        
        ServletRequestPropertyHolder(Message m) {
            super(m);
            request = (HttpServletRequest)m.get("HTTP.REQUEST");
        }

        @Override
        public Object getProperty(String name) {
            final Object value = request.getAttribute(name);

            if (value != null) {
                return value;
            }
            
            return super.getProperty(name);
        }

        @Override
        public void removeProperty(String name) {
            super.removeProperty(name);
            request.removeAttribute(name);
        }

        @Override
        public void setProperty(String name, Object value) {
            if (value == null) {
                removeProperty(name);
            } else {
                super.setProperty(name, value);
                request.setAttribute(name, value);
            }
        }

        @Override
        public Collection<String> getPropertyNames() {
            final Set<String> list = new LinkedHashSet<>();
            
            Enumeration<String> attrNames = request.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String name = attrNames.nextElement();
                if (!ENDPOINT_ADDRESS_PROPERTY.equals(name)) {
                    list.add(name);
                }
            }
            
            list.addAll(super.getPropertyNames());
            return list;
        }

    }

    private static class MessagePropertyHolder implements PropertyHolder {
        private static final String PROPERTY_KEY = "jaxrs.filter.properties";
        private Message m;
        private Map<String, Object> props;

        MessagePropertyHolder(Message m) {
            this.m = m;
            this.props = CastUtils.cast((Map<?, ?>)m.getExchange().get(PROPERTY_KEY));
        }
        public Object getProperty(String name) {
            return props == null ? null : props.get(name);
        }

        public void removeProperty(String name) {
            if (props != null) {
                props.remove(name);
            }
        }


        public void setProperty(String name, Object value) {
            if (props == null) {
                props = new HashMap<>();
                m.getExchange().put(PROPERTY_KEY, props);
            }
            if (value == null) {
                removeProperty(name);
            } else {
                props.put(name, value);
            }

        }

        public Collection<String> getPropertyNames() {
            return props == null ? Collections.<String>emptyList()
                : Collections.unmodifiableSet(props.keySet());
        }
    }
}
