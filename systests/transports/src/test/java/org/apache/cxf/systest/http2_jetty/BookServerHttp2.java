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

package org.apache.cxf.systest.http2_jetty;

public class BookServerHttp2 extends AbstractBookServerHttp2 {
    public static final String PORT = allocatePort(BookServerHttp2.class);

    org.apache.cxf.endpoint.Server server;

    public BookServerHttp2() {
        this(PORT);
    }

    public BookServerHttp2(String port) {
        super(port, "org/apache/cxf/systest/http2_jetty/server-tls.xml", "https");
    }

    public static void main(String[] args) {
        try {
            BookServerHttp2 s = new BookServerHttp2();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
