package com.x_tornado10.lccp.task_scheduler;

import com.x_tornado10.lccp.LCCP;
import com.x_tornado10.lccp.util.network.Networking;

import java.io.InputStream;

public abstract class LCCPProcessor extends LCCPRunnable {
    private InputStream is;
    private String id;
    private Networking.Communication.NetworkHandler.SuccessCallback2 callback;

    public synchronized LCCPTask runTaskAsynchronously(InputStream is) throws IllegalStateException {
        checkState();
        this.is = is;
        return setupId(LCCP.getScheduler().runTaskAsynchronously(this));
    }

    public synchronized LCCPTask runTask(InputStream is) throws IllegalStateException {
       return runTask(is, "");
    }

    public synchronized LCCPTask runTask(InputStream is, String id) throws IllegalStateException {
        checkState();
        this.is = is;
        if (id != null && !id.isEmpty()) this.id = id;
        return setupId(LCCP.getScheduler().runTask(this));
    }

    public synchronized LCCPTask runTask(InputStream is, Networking.Communication.NetworkHandler.SuccessCallback2 callback) throws IllegalStateException {
        return runTask(is, callback, "");
    }

    public synchronized LCCPTask runTask(InputStream is, Networking.Communication.NetworkHandler.SuccessCallback2 callback, String id) throws IllegalStateException {
        checkState();
        this.is = is;
        if (id != null && !id.isEmpty()) this.id = id;
        this.callback = callback;
        return setupId(LCCP.getScheduler().runTask(this));
    }

    @Override
    public void run() {
        if (callback != null) run(is, callback);
        else run(is);
    }

    public void run(InputStream is, Networking.Communication.NetworkHandler.SuccessCallback2 callback) {
    }
    public void run(InputStream is) {
    }

    @Override
    public void checkState() {
        super.checkState();
    }

    @Override
    public String toString() {
        return (id == null ? "ID[unknown]" :  "ID[" + id  +"]" ) + " with LCCPProcessor: ID[" + getTaskId() + "]";
    }
}
