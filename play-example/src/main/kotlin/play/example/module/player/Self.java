package play.example.module.player;

/**
 * <h1>禁止修改和创建实例</h1>
 * 表示玩家自己
 */
// kotlin不知支持package-private, 所以用java
public final class Self {
    public final long id;

    /**
     * keep it package-private
     */
    Self(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Self self = (Self) o;
        return id == self.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return Long.toString(id);
    }
}
