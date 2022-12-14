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

package org.apache.cxf.systest.soapfault.details;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.hello_world_soap12_http.Greeter;
import org.apache.hello_world_soap12_http.PingMeFault;
import org.apache.hello_world_soap12_http.types.FaultDetail;

@jakarta.jws.WebService(portName = "SoapPort", serviceName = "SOAPService",
                      targetNamespace = "http://apache.org/hello_world_soap12_http",
                      endpointInterface = "org.apache.hello_world_soap12_http.Greeter",
                      wsdlLocation = "wsdl/hello_world_soap12.wsdl")

public class GreeterImpl12 implements Greeter {

    private static final Logger LOG = LogUtils.getLogger(GreeterImpl12.class);

    /* (non-Javadoc)
     * @see org.apache.hello_world_soap12_http.Greeter#sayHi()
     */
    public String sayHi() {
        // throw the exception out
        Exception cause = new IllegalArgumentException("Get a wrong name <sayHi>");
        throw new Fault("sayHiFault", LOG, cause);
    }

    public void pingMe() throws PingMeFault {
        FaultDetail faultDetail = new FaultDetail();
        faultDetail.setMajor((short)2);
        faultDetail.setMinor((short)1);
        LOG.info("Executing operation pingMe, throwing PingMeFault exception");
        System.out.println("Executing operation pingMe, throwing PingMeFault exception\n");
        throw new PingMeFault("PingMeFault raised by server", faultDetail);
    }
}
