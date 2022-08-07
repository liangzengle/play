package com.esotericsoftware.kryo;

import com.google.common.collect.Lists;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;

public final class KryoReflect {

    private KryoReflect() {
    }

    private static final VarHandle defaultSerializers;

    static {
        try {
            defaultSerializers = MethodHandles.privateLookupIn(Kryo.class, MethodHandles.lookup()).findVarHandle(Kryo.class, "defaultSerializers", ArrayList.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Map.Entry<Class<?>, SerializerFactory<?>>> getDefaultSerializerFactoriesImmutableView(Kryo kryo) {
        ArrayList<Kryo.DefaultSerializerEntry> list = (ArrayList<Kryo.DefaultSerializerEntry>) defaultSerializers.get(kryo);
        return Collections.unmodifiableList(Lists.transform(list, entry -> new AbstractMap.SimpleImmutableEntry<>(entry.type, entry.serializerFactory)));
    }
}
