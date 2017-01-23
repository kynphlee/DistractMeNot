package com.ndl.distractmenot;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.ndl.distractmenot.monitor.DMNLocationMonitor;
import com.ndl.distractmenot.monitor.DMNLocationMonitor.MonitorBinder;
import com.ndl.distractmenot.monitor.MonitorState;
import com.ndl.distractmenot.monitor.SmsMonitor.MonitorListener;

//import android.support.v7.app.AppCompatActivity;

public class DMNStatus extends AppCompatActivity implements MonitorListener{

    private final static String TAG = DMNStatus.class.getSimpleName();

    private boolean receiverRegistered;
    private ImageView serviceSwitch;
    private boolean serviceEnabled;

    private DMNLocationMonitor monitorService;
    private boolean isBound = false;

    private ServiceConnection monitorConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MonitorBinder binder = (MonitorBinder) service;
            monitorService = binder.getService();
            monitorService.getMonitor().setMonitorActivity(DMNStatus.this);

            isBound = true;
            if (monitorService.getMonitor().isOverridden(MonitorState.ACTIVE)) {
                onCaptureStart();
            }

//            MonitorState state = monitorService.currentState();
//            if (state.equals(MonitorState.PASSIVE) ||
//                    state.equals(MonitorState.PASSIVE_OVERRIDE)) {
//                onPassiveModeOverrideChange();
//            }

            Log.d(TAG, "Bound to DMNLocationMonitor Service.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            monitorService.getMonitor().setMonitorActivity(null);
            isBound = false;
        }
    };

    /*-------------------------------- Lifecycle Methods --------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmnstatus);

        serviceSwitch = (ImageView) findViewById(R.id.dmn_service_switch);
        Intent initializer = new Intent(this, DMNLocationMonitor.class);
        startService(initializer);
        Log.d(TAG, "Initializing DMNLocationMonitor Service.");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent bindIntent = new Intent(this, DMNLocationMonitor.class);
        bindService(bindIntent, monitorConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Sending bind intent to DMNLocationMonitor Service...");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "state: onResume()");
        Log.d(TAG, "  *** activity started ***  ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "state: onPause()");
        Log.d(TAG, "  *** activity pause ***  ");
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(isBound) {
            unbindService(monitorConnection);
            isBound = false;
        }

//        if (receiverRegistered) {
//            unregisterReceiver(smsIntentReceiver);
//            receiverRegistered = false;
//        }
//
//        if(heartbeatReceiverRegistered) {
//            unregisterReceiver(heartbeatReceiver);
//            heartbeatReceiverRegistered = false;
//        }
        Log.d(TAG, "state: onStop()");
        Log.d(TAG, "  *** activity stopped ***  ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "state: onDestroy()");
        Log.d(TAG, "  *** activity ended ***  ");
    }
    /*-------------------------------- Active and Passive mode indicators ------------------------*/
    @Override
    public void onCaptureStart() {
        serviceSwitch.setImageResource(R.drawable.nexus_logo_blue);
        serviceEnabled = true;
    }

    @Override
    public void onCaptureStop() {
        serviceSwitch.setImageResource(R.drawable.nexus_logo_red);
        serviceEnabled = false;
    }
}
