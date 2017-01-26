package com.ndl.distractmenot.monitor;

import android.app.Activity;
import android.app.Service;
import android.location.Location;
import android.util.Log;

import com.ndl.distractmenot.DMNStatus;

import static com.ndl.distractmenot.util.DMNConstants.MS2S;
import static com.ndl.distractmenot.util.DMNConstants.ONE_MINUTE;
import static com.ndl.distractmenot.util.DMNConstants.THREE_MINUTES;
import static com.ndl.distractmenot.util.DMNConstants.THRESHOLD;
import static com.ndl.distractmenot.util.DMNUtils.longToDecimal;
import static com.ndl.distractmenot.util.DMNUtils.speedToMPH;

/* State Machine Implementation */
public class DMNStateMachine implements StateMachine {
    private DMNStatus mActivity;
    private DMNLocationMonitor mService;
    private boolean monitorActivitySet = false;
    private State passiveState;
    private State activeState;
    private State delayState;
    private State monitorState;

    private MonitorState mState;
    private MonitorListener monitorListener;

    public DMNStateMachine() {
        try {
            activeState = new ActiveState(this);
            passiveState = new PassiveState(this);
            delayState = new DelayState(this);

            mState = MonitorState.PASSIVE;
            monitorState = passiveState;

        } catch (ClassCastException ex) {
            throw new ClassCastException(mActivity.getLocalClassName()
                    + "must implement ManualOverrideInterface.");
        }
    }

    @Override
    public void setMonitorActivity(Activity context) {
        mActivity = (DMNStatus) context;
        monitorListener = mActivity;
        monitorActivitySet = context != null;
    }

    @Override
    public void setMonitorService(Service context) {
        mService = (DMNLocationMonitor) context;
    }

    @Override
    public void transitionTo(MonitorState newState) {
        switch (newState.ordinal()) {
            case 0: case 3:// Passive or Passive-Override
                mState = newState;
                monitorState = passiveState;
                break;
            case 1: // Active
                mState = newState;
                monitorState = activeState;
                break;
            case 2: // Delay
                mState = newState;
                monitorState = delayState;
                break;
        }

//        if (isOverridden(newState)) {
//            monitorState.onOverrideState();
//        }
    }

    @Override
    public MonitorState currentState() {
        return mState;
    }

    @Override
    public void setOverride(MonitorState stateToOverride, boolean override) {

    }

    @Override
    public boolean isOverridden(MonitorState state) {
        return false;
    }

    @Override
    public void sense(Location location) {
        monitorState.run(location);
    }

    /*-------------------------------- States --------------------------------*/
    abstract class State {

        protected StateMachine monitor;
//        protected boolean override;
        protected double startTime;

        public State(StateMachine monitor) {
            this.monitor = monitor;
//            override = false;
        }

//        protected void setOverride(boolean newValue) {
//            override = newValue;
//        }
//
//        protected boolean isOverridden() {
//            return override;
//        }

        protected double getDuration() {
            double currentTimeDouble = longToDecimal(System.currentTimeMillis());
            return (currentTimeDouble - startTime) * MS2S;
        }

        protected void run(final Location location) {
            if (location != null && location.hasSpeed()) {
//                if (override) {
//                    override(location);
//                } else {
//                    process(location);
//                }
                process(location);
            }
        }

//        protected void onOverrideState() {
//
//        }
//
//        protected void override(final Location location) {
//
//        }

        protected abstract void process(final Location location);
    }

    private class PassiveState extends State {
        private int threshold = speedToMPH(THRESHOLD);
        private boolean _override = false;
        private final String TAG = this.getClass().getSimpleName();

        public PassiveState(DMNStateMachine monitor) {
            super(monitor);
//            setOverride(false);
        }

//        @Override
//        protected void override(final Location location) {
////            Log.d(TAG, "State: Passive-Override");
////            int speed = speedToMPH(location);
////            if (!_override) {
////                if (speed < threshold) {
////                    return;
////                }
////                if (speed >= threshold) {
////                    _override = true;
////                }
////            } else {
////                if (speed >= threshold) {
////                    return;
////                }
////                if (speed < threshold) {
////                    _override = false;
////                    monitor.setOverride(MonitorState.PASSIVE, false);
////                }
////            }
//        }

        @Override
        protected void process(final Location location) {
            Log.d(TAG, "State: Passive");
            int speed = speedToMPH(location);
            if (speed >= threshold) {
                // Speed threshold reached. Transition to Active state.
                if (monitorActivitySet) {
                    monitorListener.onCaptureStart();
                }
                DMNCaptureService.startSMSCapture(mActivity);
                startTime = longToDecimal(System.currentTimeMillis());
                monitor.transitionTo(MonitorState.ACTIVE);
            }
        }
    }

    private class ActiveState extends State {
        private int durationLimit = THREE_MINUTES;
        private double duration = 0;
        private int threshold = speedToMPH(THRESHOLD);
        private final String TAG = this.getClass().getSimpleName();
        private boolean _override = false;

        public ActiveState(DMNStateMachine monitor) {
            super(monitor);
        }

//        @Override
//        protected void onOverrideState() {
////            DMNCaptureService.startSMSCapture(mService);
////            if (monitorActivitySet) {
////                monitorListener.onCaptureStart();
////            }
////            Log.w(TAG, "GPS Override Enabled!!!");
//        }
//
//        @Override
//        protected void override(Location location) {
////            Log.w(TAG, "GPS Override Enabled!!!");
//        }

        @Override
        protected void process(Location location) {
            Log.d(TAG, "State: Active");
            duration = getDuration();
            if (duration >= durationLimit) {
                int speed = speedToMPH(location);
                if (speed >= threshold) {
                    // Continue blocking SMS messages
                    startTime = longToDecimal(System.currentTimeMillis());
                } else if (speed == 0) {
                    startTime = longToDecimal(System.currentTimeMillis());
                    monitor.transitionTo(MonitorState.DELAY);
                }
            }
        }
    }

    private class DelayState extends State {
        private int durationLimit = ONE_MINUTE;
        private double duration = 0;
        private int threshold = speedToMPH(THRESHOLD);
        private final String TAG = this.getClass().getSimpleName();

        public DelayState(DMNStateMachine monitor) {
            super(monitor);
        }

        @Override
        protected void process(Location location) {
            Log.d(TAG, "State: Delay");
            duration = getDuration();
            if (duration >= durationLimit) {
                int speed = speedToMPH(location);

                if (speed >= threshold) {
                    startTime = longToDecimal(System.currentTimeMillis());
                    monitor.transitionTo(MonitorState.ACTIVE);
                } else if (speed == 0) {
                    DMNCaptureService.stopSMSCapture(mActivity);
                    if(monitorActivitySet) {
                        monitorListener.onCaptureStop();
                    }
                    monitor.transitionTo(MonitorState.PASSIVE);
                }
            }
        }
    }
}
