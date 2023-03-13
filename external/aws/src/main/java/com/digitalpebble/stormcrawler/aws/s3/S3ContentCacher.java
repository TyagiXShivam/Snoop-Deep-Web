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
package com.digitalpebble.stormcrawler.aws.s3;

import com.digitalpebble.stormcrawler.Metadata;

/** Caches byte[] content into S3 */
public class S3ContentCacher extends S3Cacher {

    @Override
    protected byte[] getContentToCache(Metadata metadata, byte[] content, String url) {
        if (!"true".equalsIgnoreCase(metadata.getFirstValue("http.trimmed"))) {
            return content;
        }

        LOG.info("Content was trimmed, so will not return to be cached");
        return null;
    }

    @Override
    protected String getKeyPrefix() {
        return "";
    }

    @Override
    protected String getMetricPrefix() {
        return "counters_" + getClass().getSimpleName();
    }

    @Override
    protected boolean shouldOverwrite(Metadata metadata) {
        return (!"true".equalsIgnoreCase(metadata.getFirstValue(INCACHE)));
    }
}
