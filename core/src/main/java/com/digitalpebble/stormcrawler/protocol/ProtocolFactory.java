/**
 * Licensed to DigitalPebble Ltd under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.digitalpebble.stormcrawler.protocol;

import com.digitalpebble.stormcrawler.util.ConfUtils;
import com.digitalpebble.stormcrawler.util.InitialisationUtil;
import java.net.URL;
import java.util.HashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.storm.Config;

public class ProtocolFactory {

    private final HashMap<String, Protocol[]> cache = new HashMap<>();

    private ProtocolFactory() {}

    private static volatile ProtocolFactory single_instance = null;

    public static ProtocolFactory getInstance(Config conf) {

        // https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java

        ProtocolFactory temp = single_instance;

        if (temp == null) {
            // Synchronize on class-level.
            synchronized (ProtocolFactory.class) {
                temp = single_instance;
                if (temp == null) {
                    temp = new ProtocolFactory();
                    temp.configure(conf);
                    single_instance = temp;
                }
            }
        }

        return single_instance;
    }

    // Keep initialisation in class scope.
    private void configure(Config conf) {
        // load the list of protocols
        String[] protocols = ConfUtils.getString(conf, "protocols", "http,https").split(" *, *");

        int protocolInstanceNum = ConfUtils.getInt(conf, "protocol.instances.num", 1);

        // load the class names for each protocol
        // e.g. http.protocol.implementation
        for (String protocol : protocols) {
            String paramName = protocol + ".protocol.implementation";
            String protocolimplementation = ConfUtils.getString(conf, paramName);
            if (StringUtils.isBlank(protocolimplementation)) {
                // set the default values
                if (protocol.equalsIgnoreCase("http")) {
                    protocolimplementation =
                            "com.digitalpebble.stormcrawler.protocol.httpclient.HttpProtocol";
                } else if (protocol.equalsIgnoreCase("https")) {
                    protocolimplementation =
                            "com.digitalpebble.stormcrawler.protocol.httpclient.HttpProtocol";
                } else throw new RuntimeException(paramName + "should not have an empty value");
            }
            // we have a value -> is it correct?
            Protocol[] protocolInstances = new Protocol[protocolInstanceNum];
            for (int i = 0; i < protocolInstanceNum; i++) {
                Protocol protoInstance =
                        InitialisationUtil.initializeFromQualifiedName(
                                protocolimplementation, Protocol.class);
                protoInstance.configure(conf);
                protocolInstances[i] = protoInstance;
            }
            cache.put(protocol, protocolInstances);
        }
    }

    public synchronized void cleanup() {
        cache.forEach(
                (k, v) -> {
                    for (Protocol p : v) p.cleanup();
                });
    }

    /** Returns an instance of the protocol to use for a given URL */
    public synchronized Protocol getProtocol(URL url) {
        // get the protocol
        String protocol = url.getProtocol();

        // select client from pool
        int hash = url.getHost().hashCode();
        Protocol[] pool = cache.get(protocol);
        return pool[(hash & Integer.MAX_VALUE) % pool.length];
    }

    /**
     * Returns instance(s) of the implementation for the protocol passed as argument.
     *
     * @since 1.17
     * @param protocol representation of the protocol e.g. http
     */
    public synchronized Protocol[] getProtocol(String protocol) {
        // get the protocol
        return cache.get(protocol);
    }
}
