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

package org.apache.cxf.transport.https;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;

/* ------------------------------------------------------------ */
/**
 * KeyManager to select a key with desired alias while delegating processing to specified KeyManager Can be
 * used both with server and client sockets
 */
public class AliasedX509ExtendedKeyManager extends X509ExtendedKeyManager {
    private String keyAlias;
    private X509KeyManager keyManager;

    /* ------------------------------------------------------------ */
    /**
     * Construct KeyManager instance
     *
     * @param keyAlias Alias of the key to be selected
     * @param keyManager Instance of KeyManager to be wrapped
     * @throws Exception
     */
    public AliasedX509ExtendedKeyManager(String keyAlias, X509KeyManager keyManager) throws Exception {
        this.keyAlias = keyAlias;
        this.keyManager = keyManager;
    }

    /* ------------------------------------------------------------ */
    /**
     * @see javax.net.ssl.X509KeyManager#chooseClientAlias(java.lang.String[], java.security.Principal[],
     *      java.net.Socket)
     */
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return keyAlias == null ? keyManager.chooseClientAlias(keyType, issuers, socket) : keyAlias;
    }

    /* ------------------------------------------------------------ */
    /**
     * @see javax.net.ssl.X509KeyManager#chooseServerAlias(java.lang.String, java.security.Principal[],
     *      java.net.Socket)
     */
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return keyAlias == null ? keyManager.chooseServerAlias(keyType, issuers, socket) : keyAlias;
    }

    /* ------------------------------------------------------------ */
    /**
     * @see javax.net.ssl.X509KeyManager#getClientAliases(java.lang.String, java.security.Principal[])
     */
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return keyManager.getClientAliases(keyType, issuers);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see javax.net.ssl.X509KeyManager#getServerAliases(java.lang.String, java.security.Principal[])
     */
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return keyManager.getServerAliases(keyType, issuers);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see javax.net.ssl.X509KeyManager#getCertificateChain(java.lang.String)
     */
    public X509Certificate[] getCertificateChain(String alias) {
        return keyManager.getCertificateChain(alias);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see javax.net.ssl.X509KeyManager#getPrivateKey(java.lang.String)
     */
    public PrivateKey getPrivateKey(String alias) {
        return keyManager.getPrivateKey(alias);
    }

    /* ------------------------------------------------------------ */
    /**
     * @see javax.net.ssl.X509ExtendedKeyManager#chooseEngineServerAlias(java.lang.String,
     *      java.security.Principal[], jakarta.net.ssl.SSLEngine)
     */
    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return keyAlias == null ? super.chooseEngineServerAlias(keyType, issuers, engine) : keyAlias;
    }

    /* ------------------------------------------------------------ */
    /**
     * @see javax.net.ssl.X509ExtendedKeyManager#chooseEngineClientAlias(String[], Principal[], SSLEngine)
     */
    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return keyAlias == null ? super.chooseEngineClientAlias(keyType, issuers, engine) : keyAlias;
    }
}
