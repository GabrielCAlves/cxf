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

package org.apache.cxf.systest.provider;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

public class AttachmentServer extends AbstractBusTestServerBase {
    public static final String ADDRESS
        = "http://localhost:" + TestUtil.getPortNumber(AttachmentServer.class)
            + "/XMLServiceAttachment";
    protected void run() {
        Object implementor = new AttachmentStreamSourceXMLProvider();
        Endpoint.publish(ADDRESS, implementor);
    }

    public static void main(String[] args) {
        try {
            AttachmentServer s = new AttachmentServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}