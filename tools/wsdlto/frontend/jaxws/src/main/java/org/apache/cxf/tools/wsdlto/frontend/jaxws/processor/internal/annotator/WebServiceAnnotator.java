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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator;

import jakarta.jws.WebService;
import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaInterface;

public final class WebServiceAnnotator implements Annotator {

    public void annotate(JavaAnnotatable  ja) {
        final JavaInterface intf;
        if (ja instanceof JavaInterface) {
            intf = (JavaInterface) ja;
        } else {
            throw new RuntimeException("WebService can only annotate JavaInterface");
        }
        JAnnotation serviceAnnotation = new JAnnotation(WebService.class);
        serviceAnnotation.addElement(new JAnnotationElement("targetNamespace",
                                                                   intf.getNamespace()));
        serviceAnnotation.addElement(new JAnnotationElement("name", intf.getWebServiceName()));

        intf.addAnnotation(serviceAnnotation);
    }
}

