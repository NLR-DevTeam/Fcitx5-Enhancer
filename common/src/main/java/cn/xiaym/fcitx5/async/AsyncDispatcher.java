package cn.xiaym.fcitx5.async;

import cn.xiaym.fcitx5.Fcitx5;

/**
 * An on-demand asynchronous dispatcher, which releases CPU resource when idle.
 * NOT THREAD-SAFE. Only executes the last runnable submitted.
 */
public class AsyncDispatcher extends Thread {
    private Runnable runnable;

    {
        setName("Fcitx5 AsyncDispatcher");
        setPriority(3 /* Low */);
        setDaemon(true);

        start();
    }

    @Override
    public void run() {
        while (true) {
            if (runnable == null) {
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException ignored) {
                        break;
                    }
                }

                if (runnable == null) {
                    continue;
                }
            }

            try {
                runnable.run();
            } catch (RuntimeException ex) {
                Fcitx5.LOGGER.warn("Failed to dispatch runnable!", ex);
            }

            runnable = null;
        }
    }

    public void dispatch(Runnable runnable) {
        this.runnable = runnable;

        synchronized (this) {
            this.notifyAll();
        }
    }
}
