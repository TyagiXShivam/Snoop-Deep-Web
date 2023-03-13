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
package com.digitalpebble.stormcrawler;

import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.StringArraySerializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer.BindMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/** Wrapper around Map &lt;String,String[]&gt; * */
public class Metadata {

    // customize the behaviour of Kryo via annotations
    @BindMap(
            valueSerializer = StringArraySerializer.class,
            keySerializer = StringSerializer.class,
            valueClass = String[].class,
            keyClass = String.class,
            keysCanBeNull = false)
    private Map<String, String[]> md;

    public static final Metadata empty = new Metadata(Collections.<String, String[]>emptyMap());

    public Metadata() {
        md = new HashMap<>();
    }

    private transient boolean locked = false;

    /** Wraps an existing HashMap into a Metadata object - does not clone the content */
    public Metadata(Map<String, String[]> metadata) {
        if (metadata == null) throw new NullPointerException();
        md = metadata;
    }

    /** Puts all the metadata into the current instance * */
    public void putAll(Metadata m) {
        checkLockException();

        md.putAll(m.md);
    }

    /**
     * Puts all prefixed metadata into the current instance
     *
     * @param m metadata to be added
     * @param prefix string to prefix keys in m before adding them to the current metadata. No
     *     separator is inserted between prefix and original key, so the prefix must include any
     *     separator (eg. a dot)
     */
    public void putAll(Metadata m, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            putAll(m);
            return;
        }

        Map<String, String[]> ma = m.asMap();
        ma.forEach(
                (k, v) -> {
                    setValues(prefix + k, v);
                });
    }

    /**
     * @return the first value for the key or null if it does not exist *
     */
    public String getFirstValue(String key) {
        String[] values = md.get(key);
        if (values == null) return null;
        if (values.length == 0) return null;
        return values[0];
    }

    /**
     * @return the first value for the key or null if it does not exist, given a prefix
     */
    public String getFirstValue(String key, String prefix) {
        if (prefix == null || prefix.length() == 0) return getFirstValue(key);
        return getFirstValue(prefix + key);
    }

    public String[] getValues(String key, String prefix) {
        if (prefix == null || prefix.length() == 0) return getValues(key);
        return getValues(prefix + key);
    }

    public String[] getValues(String key) {
        String[] values = md.get(key);
        if (values == null) return null;
        if (values.length == 0) return null;
        return values;
    }

    public boolean containsKey(String key) {
        return md.containsKey(key);
    }

    public boolean containsKeyWithValue(String key, String value) {
        String[] values = getValues(key);
        if (values == null) return false;
        for (String s : values) {
            if (s.equals(value)) return true;
        }
        return false;
    }

    /** Set the value for a given key. The value can be null. */
    public void setValue(String key, String value) {
        checkLockException();

        md.put(key, new String[] {value});
    }

    public void setValues(String key, String[] values) {
        checkLockException();

        if (values == null || values.length == 0) return;
        md.put(key, values);
    }

    public void addValue(String key, String value) {
        checkLockException();

        if (StringUtils.isBlank(value)) return;

        String[] existingvals = md.get(key);
        if (existingvals == null || existingvals.length == 0) {
            setValue(key, value);
            return;
        }

        int currentLength = existingvals.length;
        String[] newvals = new String[currentLength + 1];
        newvals[currentLength] = value;
        System.arraycopy(existingvals, 0, newvals, 0, currentLength);
        md.put(key, newvals);
    }

    public void addValues(String key, Collection<String> values) {
        checkLockException();

        if (values == null || values.size() == 0) return;
        String[] existingvals = md.get(key);
        if (existingvals == null) {
            md.put(key, values.toArray(new String[0]));
            return;
        }

        ArrayList<String> existing = new ArrayList<>(existingvals.length + values.size());
        Collections.addAll(existing, existingvals);

        existing.addAll(values);
        md.put(key, existing.toArray(new String[0]));
    }

    /**
     * @return the previous value(s) associated with <tt>key</tt>
     */
    public String[] remove(String key) {
        checkLockException();
        return md.remove(key);
    }

    public String toString() {
        return toString("");
    }

    /** Returns a String representation of the metadata with one K/V per line */
    public String toString(String prefix) {
        StringBuilder sb = new StringBuilder();
        if (prefix == null) prefix = "";
        Iterator<Entry<String, String[]>> iter = md.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String[]> entry = iter.next();
            for (String val : entry.getValue()) {
                sb.append(prefix).append(entry.getKey()).append(": ").append(val).append("\n");
            }
        }
        return sb.toString();
    }

    public int size() {
        return md.size();
    }

    public Set<String> keySet() {
        return md.keySet();
    }

    /** Returns the first non empty value found for the keys or null if none found. */
    public static String getFirstValue(Metadata md, String... keys) {
        for (String key : keys) {
            String val = md.getFirstValue(key);
            if (StringUtils.isBlank(val)) continue;
            return val;
        }
        return null;
    }

    /** Returns the underlying Map * */
    public Map<String, String[]> asMap() {
        return md;
    }

    /**
     * Prevents modifications to the metadata object. Useful for debugging modifications of the
     * metadata after they have been serialized. Instead of choking when serializing, a
     * ConcurrentModificationException will be thrown where the metadata are modified. <br>
     * Use like this in any bolt where you see java.lang.RuntimeException:
     * com.esotericsoftware.kryo.KryoException: java.util.ConcurrentModificationException<br>
     * collector.emit(StatusStreamName, tuple, new Values(url, metadata.lock(), Status.FETCHED));
     *
     * @since 1.16
     */
    public Metadata lock() {
        locked = true;
        return this;
    }

    /**
     * Release the lock on a metadata
     *
     * @since 1.16
     */
    public Metadata unlock() {
        locked = false;
        return this;
    }

    /**
     * @since 1.16
     */
    private void checkLockException() {
        if (locked)
            throw new ConcurrentModificationException(
                    "Attempt to modify a metadata after it has been sent to the serializer");
    }
}
