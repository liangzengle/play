/**
 * *****************************************************************************
 * Copyright 2012 Roman Levenstein
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package play.kryo;

import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.util.DefaultClassResolver;

import java.nio.charset.StandardCharsets;

class KryoClassResolver extends DefaultClassResolver {
    private final boolean logImplicits;

    KryoClassResolver(boolean logImplicits) {
        this.logImplicits = logImplicits;
    }

    @Override
    public Registration registerImplicit(Class typ) {
        if (kryo.isRegistrationRequired()) {
            throw new IllegalArgumentException("Class is not registered: " + typ.getName() + "\nNote: To register this class use: kryo.register(" + typ.getName() + ".class);");
        }
        Registration implicitRegistration = kryo.register(new Registration(typ, kryo.getDefaultSerializer(typ), hash(typ.getName().getBytes(StandardCharsets.UTF_8), 0) >>> 1));
        if (logImplicits) {
            Registration registration = kryo.getRegistration(typ);
            if (registration.getId() == DefaultClassResolver.NAME)
                System.out.println("Implicitly registered class " + typ.getName());
            else
                System.out.println("Implicitly registered class with id: " + typ.getName() + "=" + registration.getId());
        }
        return implicitRegistration;
    }

    private int hash(byte[] data, int seed) {
        final int m = 0x5bd1e995;
        final int r = 24;

        int h = seed ^ data.length;

        final int len = data.length;
        final int len_4 = len >> 2;

        var i = 0;
        while (i < len_4) {
            final int i_4 = i << 2;
            int k = data[i_4 + 3];
            k = k << 8;
            k = k | (data[i_4 + 2] & 0xff);
            k = k << 8;
            k = k | (data[i_4 + 1] & 0xff);
            k = k << 8;
            k = k | (data[i_4] & 0xff);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
            i = i + 1;
        }

        final int len_m = len_4 << 2;
        final int left = len - len_m;

        if (left != 0) {
            if (left >= 3) {
                h ^= (data[len - 3]) << 16;
            }
            if (left >= 2) {
                h ^= (data[len - 2]) << 8;
            }
            if (left >= 1) {
                h ^= (data[len - 1]);
            }

            h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }
}
