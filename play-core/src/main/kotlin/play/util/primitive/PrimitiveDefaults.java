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

    @NotNull
    public static Object get(Class<?> primitiveType) {
        switch (primitiveType.getName()) {
            case "boolean":
                return BOOLEAN;
            case "byte":
                return BYTE;
            case "short":
                return SHORT;
            case "int":
                return INTEGER;
            case "long":
                return LONG;
            case "float":
                return FLOAT;
            case "double":
                return DOUBLE;
            case "char":
                return CHARACTER;
        }
        throw new IllegalArgumentException(primitiveType.getName() + " is not a primitive class");
    }
}
