package com.ndl.distractmenot.monitor;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import static com.ndl.distractmenot.util.DMNConstants.*;

/* Location Monitor Service */
public class DMNLocationMonitor extends Service {
    private SmsMonitor monitor = null;
    private Looper serviceLooper;
    private MonitorServiceHandler serviceHandler;

    private LocationManager locationManager;
    private String locationMode = LocationManager.GPS_PROVIDER;

    private final MonitorBinder binder = new MonitorBinder();
    private final static String TAG = DMNLocationMonitor.class.getSimpleName();
    private boolean monitorServiceStarted = false;
    private boolean monitorActivityBound = false;

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread stServiceThread = new HandlerThread(
                "DMNLocationMonitorThread", Process.THREAD_PRIORITY_FOREGROUND);
        stServiceThread.start();
        serviceLooper = stServiceThread.getLooper();
        serviceHandler = new MonitorServiceHandler(serviceLooper);
        Log.d(TAG, "DMNLocationMonitor service has been created.");
    }

    public class MonitorBinder extends Binder {
        public DMNLocationMonitor getService() {
            return DMNLocationMonitor.this;
        }
    }


    private final class MonitorServiceHandler extends Handler implements LocationListener {
        public MonitorServiceHandler(Looper looper) {
            super(looper);

            // Monitor
            monitor = new DMNStateMachine();
            monitor.setMonitorService(DMNLocationMonitor.this);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "MonitorServiceHandler called.");

            if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            if (!monitorServiceStarted) {
                // GPS
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//                int fineLocationPermitted = ActivityCompat.checkSelfPermission(getBaseContext(),
//                        Manifest.permission.ACCESS_FINE_LOCATION);
//                int coarseLocationPermitted = ActivityCompat.checkSelfPermission(getBaseContext(),
//                        Manifest.permission.ACCESS_COARSE_LOCATION);

                locationManager.requestLocationUpdates(locationMode, 0, 0, MonitorServiceHandler.this);
                monitorServiceStarted = true;
                Log.d(TAG, "DMNLocationMonitor service has started");
            } else {
                Log.d(TAG, "DMNLocationMonitor service was already running.");
            }
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location != null && location.hasSpeed()) {
                Log.d(TAG, "Speed: " + location.getSpeed() +
                        " Location: " + location.getLatitude() + ", " + location.getLongitude());
                monitor.sense(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        /****************************** GPS Override start *****************************************/
        @Override
        public void onProviderEnabled(String provider) {
//            monitor.setOverride(MonitorState.ACTIVE, false);
            Log.v(TAG, "GPS is enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
//            MonitorState state = monitor.currentState();
//            if (state.equals(MonitorState.PASSIVE)
//                    || state.equals(MonitorState.DELAY)) {
//                monitor.setOverride(MonitorState.ACTIVE, true);
//                monitor.transitionTo(MonitorState.ACTIVE);
//            } else {
//                monitor.setOverride(MonitorState.ACTIVE, true);
//            }
            Log.v(TAG, "GPS is disabled");
        }
        /****************************** GPS Override end *****************************************/


    }

    /*-------------------------------- Lifecycle Methods --------------------------------*/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start listening to
        Message msg = serviceHandler.obtainMessage();
        serviceHandler.handleMessage(msg);

        return super.onStartCommand(intent, START_REDELIVER_INTENT, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        monitorServiceStarted = false;
        Log.d(DEBUG_STRING, "Service Destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        monitorActivityBound = true;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        monitorActivityBound = false;
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        monitorActivityBound = true;
    }

    /* STMonitorService Interface methods */

    public MonitorState currentState() {
        return monitor.currentState();
    }

    public SmsMonitor getMonitor() {
        return monitor;
    }
}
