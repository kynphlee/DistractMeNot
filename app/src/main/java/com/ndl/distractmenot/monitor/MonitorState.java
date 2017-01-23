package com.ndl.distractmenot.monitor;

/**
 * Created by kynphlee on 1/19/17.
 */
public enum MonitorState {

    PASSIVE("Passive"), ACTIVE("Active"), DELAY("Delay"), PASSIVE_OVERRIDE("Passive-Override");

    private final String state;

    private MonitorState(final String newState) {
        state = newState;
    }

    public String getMonitorState() {
        return state;
    }
}
