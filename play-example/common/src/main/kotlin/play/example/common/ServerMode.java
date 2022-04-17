package play.example.common;

public enum ServerMode {
    // 游戏服
    Game,
    // 场景服
    Scene,
    ;

    public boolean isGame() {
        return this == Game;
    }

    public boolean isScene() {
        return this == Scene;
    }

    public static ServerMode forName(String name) {
        switch (name.toLowerCase()) {
            case "game":
                return Game;
            case "scene":
                return Scene;
        }
        throw new IllegalStateException("Unknown server mode: " + name);
    }

    @Override
    public String toString() {
        return switch (this) {
            case Game -> "game";
            case Scene -> "scene";
        };
    }
}
