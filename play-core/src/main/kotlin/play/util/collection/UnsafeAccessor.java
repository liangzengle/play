package play.util.collection;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

final class UnsafeAccessor {
    public static final UnsafeAccessor UNSAFE_ACCESSOR = new UnsafeAccessor();

    private final Unsafe unsafe = getUnsafe();

    private UnsafeAccessor() {
    }

    private static Unsafe getUnsafe() {
        try {
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            return (Unsafe) theUnsafeField.get(null);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    long objectFieldOffset(Class<?> clazz, String fieldName) {
        try {
            return unsafe.objectFieldOffset(clazz.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    public int arrayBaseOffset(Class<?> aClass) {
        return unsafe.arrayBaseOffset(aClass);
    }

    public int arrayIndexScale(Class<?> aClass) {
        return unsafe.arrayIndexScale(aClass);
    }

    public boolean compareAndSetInt(Object object, long offset, int expect, int newValue) {
        return unsafe.compareAndSwapInt(object, offset, expect, newValue);
    }

    public Object getReferenceAcquire(Object object, long offset) {
        return unsafe.getObject(object, offset);
    }

    public boolean compareAndSetReference(Object object, long offset, Object expect, Object newValue) {
        return unsafe.compareAndSwapObject(object, offset, expect, newValue);
    }

    public void putReferenceRelease(Object object, long offset, Object newValue) {
        unsafe.putObject(object, offset, newValue);
    }

    public boolean compareAndSetLong(Object object, long offset, long expect, long newValue) {
        return unsafe.compareAndSwapLong(object, offset, expect, newValue);
    }

    public int getAndAddInt(Object object, long offset, int delta) {
        return unsafe.getAndAddInt(object, offset, delta);
    }
}
