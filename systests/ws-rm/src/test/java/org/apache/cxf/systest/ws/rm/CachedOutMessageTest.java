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

package org.apache.cxf.systest.ws.rm;

import java.util.logging.Logger;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.rm.RMManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests the WS-RM processing with the cached out message (using temporary files).
 */
public class CachedOutMessageTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);
    public static final String DECOUPLE_PORT = allocatePort("decoupled.port");

    private static final Logger LOG = LogUtils.getLogger(RetransmissionQueueTest.class);
    private Bus bus;



    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;
        protected void run()  {
            SpringBusFactory bf = new SpringBusFactory();
            Bus bus = bf.createBus("/org/apache/cxf/systest/ws/rm/message-loss.xml");
            BusFactory.setDefaultBus(bus);
            setBus(bus);
            LoggingInInterceptor in = new LoggingInInterceptor();
            bus.getInInterceptors().add(in);
            bus.getInFaultInterceptors().add(in);
            LoggingOutInterceptor out = new LoggingOutInterceptor();
            bus.getOutInterceptors().add(out);
            bus.getOutFaultInterceptors().add(out);

            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";

            ep = Endpoint.create(implementor);
            ep.publish(address);

            LOG.info("Published greeter endpoint.");
        }

        public void tearDown() {
            ep.stop();
            ep = null;
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        CachedOutputStream.setDefaultThreshold(16);
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        CachedOutputStream.setDefaultThreshold(-1);
    }

    @Test
    public void testCachedOutMessage() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/org/apache/cxf/systest/ws/rm/message-loss.xml");
        BusFactory.setDefaultBus(bus);
        LoggingInInterceptor in = new LoggingInInterceptor();
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        bus.getOutInterceptors().add(out);
        // an interceptor to simulate a message loss
        MessageLossSimulator mls = new MessageLossSimulator();
        bus.getOutInterceptors().add(mls);
        RMManager manager = bus.getExtension(RMManager.class);
        manager.getConfiguration().setBaseRetransmissionInterval(Long.valueOf(2000));

        bus.getOutFaultInterceptors().add(out);

        GreeterService gs = new GreeterService();
        final Greeter greeter = gs.getGreeterPort();
        updateAddressPort(greeter, PORT);
        LOG.fine("Created greeter client.");

        ConnectionHelper.setKeepAliveConnection(greeter, true);

        greeter.greetMeOneWay("one");
        greeter.greetMeOneWay("two");
        greeter.greetMeOneWay("three");

        long wait = 4000;
        while (wait > 0) {
            long start = System.currentTimeMillis();
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ex) {
                // ignore
            }
            wait -= System.currentTimeMillis() - start;
        }

        boolean empty = manager.getRetransmissionQueue().isEmpty();
        assertTrue("Some messages are not acknowledged", empty);
    }

}
