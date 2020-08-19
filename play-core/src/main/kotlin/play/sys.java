package play;

import java.io.File;

public final class sys {
    private sys() {
    }

    public static Runtime runtime() {
        return Runtime.getRuntime();
    }

    public static int availableProcessors() {
        return runtime().availableProcessors();
    }

    public static boolean isWindows() {
        return isOS("Windows");
    }

    public static boolean isMac() {
        return isOS("Mac OS");
    }

    public static boolean isLinux() {
        return isOS("Linux");
    }

    private static boolean isOS(String osName) {
        return SystemProperties.osName().startsWith(osName);
    }

    public String fileSeparator() {
        return File.separator;
    }

    public String lineSeparator() {
        return System.lineSeparator();
    }
}
