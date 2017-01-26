package com.ndl.distractmenot.monitor;

import android.app.Activity;
import android.app.Service;
import android.location.Location;

/**
 * Created by kynphlee on 1/19/17.
 */
public interface StateMachine {

    public interface MonitorListener {
        public void onCaptureStart();
        public void onCaptureStop();
    }

    public void setMonitorActivity(Activity context);

    public void setMonitorService(Service context);

    public void transitionTo(MonitorState newState);

    public MonitorState currentState();

    public void setOverride(MonitorState stateToOverride, boolean override);

    public boolean isOverridden(MonitorState state);

    public void sense(Location location);
}
