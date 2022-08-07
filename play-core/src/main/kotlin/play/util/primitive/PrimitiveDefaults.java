package play.util.primitive;

import org.jetbrains.annotations.NotNull;

/**
 * @author LiangZengle
 */
public final class PrimitiveDefaults {
    private PrimitiveDefaults() {
        throw new UnsupportedOperationException();
    }

    public static final Boolean BOOLEAN = Boolean.FALSE;
    public static final Byte BYTE = 0;
    public static final Short SHORT = 0;
    public static final Integer INTEGER = 0;
    public static final Long LONG = 0L;
    public static final Float FLOAT = 0F;
    public static final Double DOUBLE = 0D;
    public static final Character CHARACTER = '0';

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> T get(Class<?> primitiveType) {
        switch (primitiveType.getName()) {
            case "boolean":
                return (T) BOOLEAN;
            case "byte":
                return (T) BYTE;
            case "short":
                return (T) SHORT;
            case "int":
                return (T) INTEGER;
            case "long":
                return (T) LONG;
            case "float":
                return (T) FLOAT;
            case "double":
                return (T) DOUBLE;
            case "char":
                return (T) CHARACTER;
        }
        throw new IllegalArgumentException(primitiveType.getName() + " is not a primitive class");
    }
}
