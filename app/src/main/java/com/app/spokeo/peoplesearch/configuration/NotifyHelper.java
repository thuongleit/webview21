package com.app.spokeo.peoplesearch.configuration;

import java.util.Observable;

public class NotifyHelper extends Observable {
    public static final String CONFIG_FETCHED = "com.ascendik.screenfilterlibrary.util.CONFIG_FETCHED";

    private static NotifyHelper mInstance;

    private NotifyHelper() {
        setChanged();
    }

    public static void removeInstance() {
        mInstance = null;
    }

    public static NotifyHelper getInstance() {
        if (mInstance == null) {
            synchronized (NotifyHelper.class) {
                if (mInstance == null) {
                    mInstance = new NotifyHelper();
                }
            }
        }
        return mInstance;
    }

    public void notifyObservers(String action) {
        notifyObservers(new NotifyEvent(action));
    }

    public void notifyObservers(String action, Object data) {
        notifyObservers(new NotifyEvent(action, data));
    }

    @Override
    protected void clearChanged() {
    }

    public class NotifyEvent {
        public String action;
        public Object data;

        NotifyEvent(String action, Object data) {
            this.action = action;
            this.data = data;
        }

        NotifyEvent(String action) {
            this.action = action;
        }
    }
}
