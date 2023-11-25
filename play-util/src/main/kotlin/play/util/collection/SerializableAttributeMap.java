/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package play.util.collection;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.netty.util.AttributeMap;
import io.netty.util.internal.ObjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.util.ClassUtil;
import play.util.json.Json;
import play.util.reflect.Reflect;

import java.io.IOException;
import java.io.Serial;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

/**
 * Default {@link AttributeMap} implementation which not exibit any blocking behaviour on attribute lookup while using a
 * copy-on-write approach on the modify path.<br> Attributes lookup and remove exibit {@code O(logn)} time worst-case
 * complexity, hence {@code attribute::set(null)} is to be preferred to {@code remove}.
 */
public class SerializableAttributeMap {

    private static final AtomicReferenceFieldUpdater<SerializableAttributeMap, DefaultAttribute[]> ATTRIBUTES_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(SerializableAttributeMap.class, DefaultAttribute[].class, "attributes");
    private static final DefaultAttribute<?>[] EMPTY_ATTRIBUTES = new DefaultAttribute[0];


    /**
     * Similarly to {@code Arrays::binarySearch} it perform a binary search optimized for this use case, in order to
     * save polymorphic calls (on comparator side) and unnecessary class checks.
     */
    private static int searchAttributeByKey(DefaultAttribute<?>[] sortedAttributes, SerializableAttributeKey<?> key) {
        int low = 0;
        int high = sortedAttributes.length - 1;

        while (low <= high) {
            int mid = low + high >>> 1;
            DefaultAttribute<?> midVal = sortedAttributes[mid];
            SerializableAttributeKey<?> midValKey = midVal.key;
            if (midValKey == key) {
                return mid;
            }
            int midValKeyId = midValKey.id();
            int keyId = key.id();
            assert midValKeyId != keyId;
            boolean searchRight = midValKeyId < keyId;
            if (searchRight) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        return -(low + 1);
    }

    private static void orderedCopyOnInsert(DefaultAttribute<?>[] sortedSrc, int srcLength, DefaultAttribute<?>[] copy,
                                            DefaultAttribute<?> toInsert) {
        // let's walk backward, because as a rule of thumb, toInsert.key.id() tends to be higher for new keys
        final int id = toInsert.key.id();
        int i;
        for (i = srcLength - 1; i >= 0; i--) {
            DefaultAttribute<?> attribute = sortedSrc[i];
            assert attribute.key.id() != id;
            if (attribute.key.id() < id) {
                break;
            }
            copy[i + 1] = sortedSrc[i];
        }
        copy[i + 1] = toInsert;
        final int toCopy = i + 1;
        if (toCopy > 0) {
            System.arraycopy(sortedSrc, 0, copy, 0, toCopy);
        }
    }

    private volatile DefaultAttribute<?>[] attributes = EMPTY_ATTRIBUTES;

    @SuppressWarnings("unchecked")
    public <T> SerializableAttribute<T> attr(SerializableAttributeKey<T> key) {
        ObjectUtil.checkNotNull(key, "key");
        DefaultAttribute<?> newAttribute = null;
        for (; ; ) {
            final DefaultAttribute<?>[] attributes = this.attributes;
            final int index = searchAttributeByKey(attributes, key);
            final DefaultAttribute<?>[] newAttributes;
            if (index >= 0) {
                final DefaultAttribute<?> attribute = attributes[index];
                assert attribute.key() == key;
                if (!attribute.isRemoved()) {
                    return (SerializableAttribute<T>) attribute;
                }
                // let's try to replace the removed attribute with a new one
                if (newAttribute == null) {
                    newAttribute = new DefaultAttribute<>(this, key);
                }
                final int count = attributes.length;
                newAttributes = Arrays.copyOf(attributes, count);
                newAttributes[index] = newAttribute;
            } else {
                if (newAttribute == null) {
                    newAttribute = new DefaultAttribute<T>(this, key);
                }
                final int count = attributes.length;
                newAttributes = new DefaultAttribute[count + 1];
                orderedCopyOnInsert(attributes, count, newAttributes, newAttribute);
            }
            if (ATTRIBUTES_UPDATER.compareAndSet(this, attributes, newAttributes)) {
                return (SerializableAttribute<T>) newAttribute;
            }
        }
    }

    public <T> boolean hasAttr(SerializableAttributeKey<T> key) {
        ObjectUtil.checkNotNull(key, "key");
        return searchAttributeByKey(attributes, key) >= 0;
    }

    private <T> void removeAttributeIfMatch(SerializableAttributeKey<T> key, DefaultAttribute<T> value) {
        for (; ; ) {
            final DefaultAttribute<?>[] attributes = this.attributes;
            final int index = searchAttributeByKey(attributes, key);
            if (index < 0) {
                return;
            }
            final DefaultAttribute<?> attribute = attributes[index];
            assert attribute.key() == key;
            if (attribute != value) {
                return;
            }
            final int count = attributes.length;
            final int newCount = count - 1;
            final DefaultAttribute<?>[] newAttributes =
                    newCount == 0 ? EMPTY_ATTRIBUTES : new DefaultAttribute[newCount];
            // perform 2 bulk copies
            System.arraycopy(attributes, 0, newAttributes, 0, index);
            final int remaining = count - index - 1;
            if (remaining > 0) {
                System.arraycopy(attributes, index + 1, newAttributes, index, remaining);
            }
            if (ATTRIBUTES_UPDATER.compareAndSet(this, attributes, newAttributes)) {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static final class DefaultAttribute<T> extends AtomicReference<Object> implements SerializableAttribute<T> {

        private static final AtomicReferenceFieldUpdater<DefaultAttribute, SerializableAttributeMap> MAP_UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(DefaultAttribute.class, SerializableAttributeMap.class, "attributeMap");
        @Serial
        private static final long serialVersionUID = -2661411462200283011L;

        private volatile SerializableAttributeMap attributeMap;
        private final SerializableAttributeKey<T> key;

        DefaultAttribute(SerializableAttributeMap attributeMap, SerializableAttributeKey<T> key) {
            this.attributeMap = attributeMap;
            this.key = key;
        }

        @NotNull
        @Override
        public SerializableAttributeKey<T> key() {
            return key;
        }

        @Nullable
        @Override
        public T getValue() {
            var value = get();
            if (value == null) {
                return null;
            }
            Class<?> valueRawType = null;
            for (; ; ) {
                if (valueRawType == null) {
                    valueRawType = Reflect.getRawClass(key.valueType());
                    if (valueRawType.isPrimitive()) {
                        valueRawType = ClassUtil.getPrimitiveWrapperType(valueRawType);
                    }
                }
                var valueClass = value.getClass();
                if (valueRawType.isAssignableFrom(valueClass)) {
                    return (T) value;
                }
                var jsonValue = ((JsonData) value).value;
                if (compareAndSet(value, jsonValue)) {
                    value = Json.convert(jsonValue, key.valueType());
                    set(value);
                    return (T) value;
                }
                value = get();
                if (value == null) {
                    return null;
                }
            }
        }

        @NotNull
        @Override
        public T computeIfAbsent(@NotNull Supplier<T> supplier) {
            var value = get();
            Class<?> valueRawType = null;
            for (; ; ) {
                if (value == null) {
                    if (compareAndSet(null, supplier)) {
                        value = supplier.get();
                        set(value);
                        break;
                    }
                    value = get();
                    continue;
                }
                if (valueRawType == null) {
                    valueRawType = Reflect.getRawClass(key.valueType());
                    if (valueRawType.isPrimitive()) {
                        valueRawType = ClassUtil.getPrimitiveWrapperType(valueRawType);
                    }
                }
                var valueClass = value.getClass();
                if (valueRawType.isAssignableFrom(valueClass)) {
                    return (T) value;
                }
                if (value.getClass() == JsonData.class) {
                    var jsonValue = ((JsonData) value).value;
                    if (compareAndSet(value, jsonValue)) {
                        value = Json.convert(jsonValue, key.valueType());
                        set(value);
                        return (T) value;
                    }
                }
                value = get();
            }
            return (T) value;
        }

        @Override
        public void setValue(@Nullable T value) {
            set(value);
        }

        private boolean isRemoved() {
            return attributeMap == null;
        }

        public T setIfAbsent(T value) {
            while (!compareAndSet(null, value)) {
                T old = getValue();
                if (old != null) {
                    return old;
                }
            }
            return null;
        }

        public void remove() {
            final var attributeMap = this.attributeMap;
            final boolean removed = attributeMap != null && MAP_UPDATER.compareAndSet(this, attributeMap, null);
            set(null);
            if (removed) {
                attributeMap.removeAttributeIfMatch(key, this);
            }
        }
    }

    private record JsonData(JsonNode value) {
    }


    public static class SerializableAttributeMapSerializer extends StdSerializer<SerializableAttributeMap> {


        public SerializableAttributeMapSerializer() {
            super(SerializableAttributeMap.class);
        }

        @SuppressWarnings("ForLoopReplaceableByForEach")
        @Override
        public void serialize(SerializableAttributeMap value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            var attribute = value.attributes;
            gen.writeStartObject();
            for (int i = 0; i < attribute.length; i++) {
                DefaultAttribute<?> attr = attribute[i];
                var k = attr.key.name();
                var v = attr.get();
                if (v != null) {
                    gen.writeFieldName(k);
                    gen.writeObject(v);
                }
            }
            gen.writeEndObject();
        }
    }

    public static class SerializableAttributeMapDeserializer extends StdDeserializer<SerializableAttributeMap> {
        public SerializableAttributeMapDeserializer() {
            super(SerializableAttributeMap.class);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public SerializableAttributeMap deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            var t = p.currentToken();
            if (t == JsonToken.START_OBJECT) {
                t = p.nextToken();
            }
            if (t != JsonToken.FIELD_NAME && t != JsonToken.END_OBJECT) {
                return (SerializableAttributeMap) ctxt.handleUnexpectedToken(SerializableAttributeMap.class, p);
            }
            var map = new SerializableAttributeMap();
            for (; p.currentToken() == JsonToken.FIELD_NAME; p.nextToken()) {
                var fieldName = p.currentName();
                SerializableAttributeKey key = SerializableAttributeKey.valueOf(fieldName, Void.TYPE);
                p.nextToken();
                var value = ctxt.readTree(p);
                assert value != null;
                map.attr(key).setValue(new JsonData(value));
            }
            return map;
        }
    }
}
