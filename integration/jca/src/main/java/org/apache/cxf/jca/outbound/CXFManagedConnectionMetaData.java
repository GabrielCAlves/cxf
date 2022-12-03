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
package org.apache.cxf.jca.outbound;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionMetaData;

/**
 * CXF Managed Connection MetaData
 */
public class CXFManagedConnectionMetaData implements ManagedConnectionMetaData {

    private String userName;

    /**
     * @param userName
     */
    public CXFManagedConnectionMetaData(String userName) {
        this.userName = userName;
    }


    public String getEISProductName() throws ResourceException {
        return "Apache CXF";
    }


    public String getEISProductVersion() throws ResourceException {
        return "2.0";
    }

    /*
     * Don't have a hard limit
     */
    public int getMaxConnections() throws ResourceException {
        return -1;
    }


    public String getUserName() throws ResourceException {
        return userName;
    }

}
