package com.formkiq.module.lambdaservices.timer;

import com.formkiq.module.lambdaservices.logger.LogLevel;
import com.formkiq.module.lambdaservices.logger.Logger;

/**
 * Method Timer.
 */
public class MethodTimer {

    /** Timer Name. */
    private String timerName;
    /** Start of Timer. */
    private long startTime = -1;
    /** End of Timer. */
    private long endTime = -1;
    /**
     * constructor.
     */
    public MethodTimer() {
    }

    /**
     * Start Timer.
     * @param name {@link String}
     * @return MethodTimer
     */
    public static MethodTimer startTimer(final String name) {
        return new MethodTimer().start(name);
    }

    /**
     * Start Timer.
     * @param name {@link String}
     * @return MethodTimer
     */
    public MethodTimer start(final String name) {
        this.timerName = name;
        this.startTime = System.currentTimeMillis();
        return this;
    }

    /**
     * Stop Timer.
     * @return MethodTimer
     */
    public MethodTimer stop() {
        if (startTime < 0) {
            throw new IllegalStateException("Timer has not been started.");
        }
        endTime = System.currentTimeMillis();
        return this;
    }

    /**
     * Returns the elapsed time in nanoseconds.
     * @return elapsed time in nanoseconds.
     * @throws IllegalStateException if the timer is still running.
     */
    public long getElapsedTime() {
        if (endTime < 0) {
            throw new IllegalStateException("Timer is still running. Stop it before getting the elapsed time.");
        }
        return endTime - startTime;
    }

    /**
     * Log the Elapsed Time.
     * @param logger {@link Logger}
     */
    public void logElapsedTime(final Logger logger) {
        if (logger.isLogged(LogLevel.TRACE)) {
            logger.trace(String.format("%s: elapsed time in milliseconds: %d", this.timerName, getElapsedTime()));
        }
    }
}
