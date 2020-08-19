package play.util.scheduling.executor;

import java.util.concurrent.ScheduledExecutorService;

public abstract class ScheduledExecutor {
    private ScheduledExecutor() {
    }

    private static class Holder {
        private static final ScheduledExecutorService service = new MillisBasedScheduledThreadPoolExecutor(1, 10000);
    }

    public static ScheduledExecutorService get() {
        return Holder.service;
    }
}
