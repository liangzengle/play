package play.example.common;

public enum ServerMode {
    Local,
    Remote,
    ;

    public boolean isLocal() {
        return this == Local;
    }

    public boolean isRemote() {
        return this == Remote;
    }

    public static ServerMode forName(String name) {
        switch (name.toLowerCase()) {
            case "local":
                return Local;
            case "remote":
                return Remote;
        }
        throw new IllegalStateException("Unknown server mode: " + name);
    }

    @Override
    public String toString() {
        switch (this) {
            case Local:
                return "local";
            case Remote:
                return "remote";
        }
        throw new Error("should not happen");
    }
}
