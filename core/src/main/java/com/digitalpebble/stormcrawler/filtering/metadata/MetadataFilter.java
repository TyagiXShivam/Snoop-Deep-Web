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
package com.digitalpebble.stormcrawler.filtering.metadata;

import com.digitalpebble.stormcrawler.Metadata;
import com.digitalpebble.stormcrawler.filtering.URLFilter;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Filter out URLs based on metadata in the source document */
public class MetadataFilter extends URLFilter {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataFilter.class);

    private final LinkedList<String[]> mdFilters = new LinkedList<>();

    @Override
    public void configure(@NotNull Map<String, Object> stormConf, @NotNull JsonNode paramNode) {
        java.util.Iterator<Entry<String, JsonNode>> iter = paramNode.fields();
        while (iter.hasNext()) {
            Entry<String, JsonNode> entry = iter.next();
            String key = entry.getKey();
            String value = entry.getValue().asText();
            mdFilters.add(new String[] {key, value});
        }
    }

    @Override
    public @Nullable String filter(
            @Nullable URL pageUrl, @Nullable Metadata sourceMetadata, @NotNull String urlToFilter) {
        if (sourceMetadata == null) {
            return urlToFilter;
        }
        // check whether any of the metadata can be found in the source
        Iterator<String[]> iter = mdFilters.iterator();
        while (iter.hasNext()) {
            String[] kv = iter.next();
            String[] vals = sourceMetadata.getValues(kv[0]);
            if (vals == null) {
                continue;
            }
            for (String v : vals) {
                if (v.equalsIgnoreCase(kv[1])) {
                    LOG.debug("Filtering {} matching metadata {}:{}", urlToFilter, kv[0], kv[1]);
                    return null;
                }
            }
        }
        return urlToFilter;
    }
}
