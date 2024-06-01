package com.x_tornado10.lccp.task_scheduler;

import com.x_tornado10.lccp.LCCP;

public abstract class LCCPRunnable implements Runnable {
    private int taskId = -1;

    public synchronized void cancel() throws IllegalStateException {
        LCCP.getScheduler().cancelTask(getTaskId());
    }

    public synchronized LCCPTask runTask() throws IllegalStateException {
        checkState();
        return setupId(LCCP.getScheduler().runTask(this));
    }

    public synchronized LCCPTask runTaskAsynchronously() throws IllegalStateException {
        checkState();
        return setupId(LCCP.getScheduler().runTaskAsynchronously(this));
    }

    public synchronized LCCPTask runTaskLater(long delay) throws IllegalStateException {
        checkState();
        return setupId(LCCP.getScheduler().runTaskLater(this, delay));
    }

    public synchronized LCCPTask runTaskLaterAsynchronously(long delay) throws IllegalStateException {
        checkState();
        return setupId(LCCP.getScheduler().runTaskLaterAsynchronously(this, delay));
    }

    public synchronized LCCPTask runTaskTimer(long delay, long period) throws IllegalStateException {
        checkState();
        return setupId(LCCP.getScheduler().runTaskTimer(this, delay, period));
    }

    public synchronized LCCPTask runTaskTimerAsynchronously(long delay, long period) throws IllegalStateException {
        checkState();
        return setupId(LCCP.getScheduler().runTaskTimerAsynchronously(this, delay, period));
    }

    public synchronized int getTaskId() throws IllegalStateException {
        final int id = taskId;
        if (id == -1) {
            throw new IllegalStateException("Not scheduled yet");
        }
        return id;
    }

    private void checkState() {
        if (taskId != -1) {
            throw new IllegalStateException("Already scheduled as " + taskId);
        }
    }

    private LCCPTask setupId(final LCCPTask task) {
        this.taskId = task.getTaskId();
        return task;
    }
}