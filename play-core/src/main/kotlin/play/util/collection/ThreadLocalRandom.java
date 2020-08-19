package play.util.collection;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class ThreadLocalRandom extends Random {
    private static final ThreadLocal<Tlr> tlr = ThreadLocal.withInitial(Tlr::new);
    private static final AtomicInteger probeGenerator = new AtomicInteger();
    private static final AtomicLong seeder = new AtomicLong();

    public static int getProbe() {
        return tlr.get().threadLocalRandomProbe;
    }

    public static void localInit() {
        int p = probeGenerator.addAndGet(-1640531527);
        int probe = p == 0 ? 1 : p;
        long seed = mix64(seeder.getAndAdd(-4942790177534073029L));
        Tlr t = tlr.get();
        t.threadLocalRandomProbe = probe;
        t.threadLocalRandomSeed = seed;
    }

    public static int advanceProbe(int probe) {
        probe ^= probe << 13;
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        tlr.get().threadLocalRandomProbe = probe;
        return probe;
    }

    private static long mix64(long z) {
        z = (z ^ z >>> 33) * -49064778989728563L;
        z = (z ^ z >>> 33) * -4265267296055464877L;
        return z ^ z >>> 33;
    }

    static class Tlr {
        long threadLocalRandomSeed;
        int threadLocalRandomProbe;
    }
}
