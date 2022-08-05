package play.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author LiangZengle
 */
public final class PrimitiveTypes {
    private PrimitiveTypes() {
    }

    public static final Boolean DEFAULT_BOOLEAN = Boolean.FALSE;
    public static final Byte DEFAULT_BYTE = 0;
    public static final Short DEFAULT_SHORT = 0;
    public static final Integer DEFAULT_INTEGER = 0;
    public static final Long DEFAULT_LONG = 0L;
    public static final Float DEFAULT_FLOAT = 0F;
    public static final Double DEFAULT_DOUBLE = 0D;
    public static final Character DEFAULT_CHARACTER = '0';

    @Nullable
    public static Class<?> getPrimitiveType(Class<?> type) {
        return switch (type.getName()) {
            case "int", "long", "byte", "boolean", "double", "short", "float", "char" -> type;
            case "java.lang.Boolean" -> Boolean.TYPE;
            case "java.lang.Character" -> Character.TYPE;
            case "java.lang.Byte" -> Byte.TYPE;
            case "java.lang.Short" -> Short.TYPE;
            case "java.lang.Integer" -> Integer.TYPE;
            case "java.lang.Long" -> Long.TYPE;
            case "java.lang.Float" -> Float.TYPE;
            case "java.lang.Double" -> Double.TYPE;
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static <T> T getPrimitiveDefaultValue(Class<?> primitiveType) {
        switch (primitiveType.getName()) {
            case "boolean":
                return (T) DEFAULT_BOOLEAN;
            case "byte":
                return (T) DEFAULT_BYTE;
            case "short":
                return (T) DEFAULT_SHORT;
            case "int":
                return (T) DEFAULT_INTEGER;
            case "long":
                return (T) DEFAULT_LONG;
            case "float":
                return (T) DEFAULT_FLOAT;
            case "double":
                return (T) DEFAULT_DOUBLE;
            case "char":
                return (T) DEFAULT_CHARACTER;
        }
        throw new IllegalArgumentException(primitiveType.getName() + " is not a primitive class");
    }
}
