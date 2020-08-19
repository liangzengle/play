package play;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;

public final class SystemProperties {
    private SystemProperties() {
    }

    /**
     * 如果属性不存在则设置
     *
     * @param key   属性key
     * @param value 属性值
     * @return 原来的属性值，不存在为null
     */
    @Nullable
    public static String setIfAbsent(String key, @Nonnull Object value) {
        return (String) System.getProperties().putIfAbsent(key, value.toString());
    }

    /**
     * 设置属性值
     *
     * @param key   属性key
     * @param value 属性值
     * @return 原来的属性值，不存在为null
     */
    @Nullable
    public static String set(String key, @Nonnull Object value) {
        return (String) System.getProperties().put(key, value.toString());
    }

    public static Properties getAll() {
        return System.getProperties();
    }

    @Nullable
    public static String getOrNull(String key) {
        return System.getProperties().getProperty(key);
    }

    @Nonnull
    public static String getOrThrow(String key) {
        String value = System.getProperties().getProperty(key);
        if (value == null) throw new NoSuchElementException(key);
        return value;
    }

    @Nonnull
    public static String getOrEmpty(String key) {
        return getOrDefault(key, "");
    }

    public static String getOrDefault(String key, @Nonnull String defaultValue) {
        return System.getProperties().getProperty(key, defaultValue);
    }

    public static Optional<String> get(String key) {
        return Optional.ofNullable(System.getProperties().getProperty(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(getOrThrow(key));
    }

    public static int getInt(String key, int defaultValue) {
        String value = getOrNull(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public static double getDouble(String key) {
        return Double.parseDouble(getOrThrow(key));
    }

    public static double getDouble(String key, double defaultValue) {
        String value = getOrNull(key);
        return value == null ? defaultValue : Double.parseDouble(value);
    }

    public static boolean getBoolean(String key) {
        return parseBoolean(getOrThrow(key));
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = getOrNull(key);
        return value == null ? defaultValue : parseBoolean(value);
    }

    private static boolean parseBoolean(String value) {
        switch (value) {
            case "1":
            case "true":
            case "on":
            case "enable":
                return true;
            default:
                return false;
        }
    }

    public static String osName() {
        return getOrEmpty("os.name");
    }

    public static String userDir() {
        return getOrEmpty("user.dir");
    }
}


