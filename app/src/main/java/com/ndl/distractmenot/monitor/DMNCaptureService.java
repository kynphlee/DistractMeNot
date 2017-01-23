package com.ndl.distractmenot.monitor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.ndl.distractmenot.R;

import static com.ndl.distractmenot.util.DMNConstants.*;
/**
 * Created by kynphlee on 1/20/17.
 */
/* SMS Capture Service */
public class DMNCaptureService extends Service {

    private HandlerThread smsCaptureThread;
    private Looper smsCaptureLooper;
    private SMSCaptureHandler smsCaptureHandler;
    private SmsBuffer smsBuffer;

    private int smsCount = 0;
    private boolean smsReceiverRegistered = false;
    private boolean mmsReceiverRegistered = false;

    @Override
    public void onCreate() {
        smsCaptureThread = new HandlerThread("SMSCaptureThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        smsCaptureThread.start();

        smsBuffer = new SmsBuffer(this);
        smsCaptureLooper = smsCaptureThread.getLooper();
        smsCaptureHandler = new SMSCaptureHandler(smsCaptureLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(ST_START_CAPTURE)) {
                if (!smsReceiverRegistered) {
                    Message message = smsCaptureHandler.obtainMessage();
                    smsCaptureHandler.sendMessage(message);
                }
            } else if (action.equals(ST_STOP_CAPTURE)) {
                unregisterReceiver(smsReceiver);
                unregisterReceiver(mmsReceiver);
                smsReceiverRegistered = false;
                mmsReceiverRegistered = false;

                if (smsBuffer.count() > 0) {
                    final int messagesDumped = smsBuffer.dumpSmsMessages();

                    Intent smsReceivedIntent = new Intent();
                    smsReceivedIntent.setAction(ST_PASSIVE_STATE);
                    smsReceivedIntent
                            .putExtra("messagesDumped", messagesDumped);
                    sendBroadcast(smsReceivedIntent);
                }
                stopSelf();
            }
        }
        return START_NOT_STICKY;
    }

    private final class SMSCaptureHandler extends Handler {
        public SMSCaptureHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // Start the SMS capture service here...
            IntentFilter smsFilter = new IntentFilter();
            IntentFilter mmsFilter = new IntentFilter();

            try {
                smsFilter.addAction(SMS_RECEIVED_ACTION);
                smsFilter.setPriority(Integer.MAX_VALUE);

                mmsFilter.addAction(MMS_RECEIVED_ACTION);
                mmsFilter.addDataType("application/vnd.wap.mms-message");
            } catch (IntentFilter.MalformedMimeTypeException e) {
                e.printStackTrace();
            }

            registerReceiver(smsReceiver, smsFilter);
            registerReceiver(mmsReceiver, mmsFilter);
            smsReceiverRegistered = true;
            mmsReceiverRegistered = true;
        }
    }

    private final BroadcastReceiver mmsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String type = intent.getType();
            String DEBUG_TAG = getClass().getSimpleName().toString();

            String sender = null;
            String message = null;
            SmsMessage[] messages = null;


            if (action.equals(MMS_RECEIVED_ACTION) &&
                    type.equals("application/vnd.wap.mms-message")) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Toast.makeText(context, "MMS Received", Toast.LENGTH_SHORT).show();
                   /* byte[] buffer = bundle.getByteArray("data");
                    Log.d(DEBUG_TAG, "buffer " + buffer);
                    String incomingNumber = new String(buffer);
                    int indx = incomingNumber.indexOf("/TYPE");
                    if(indx>0 && (indx-15)>0){
                        int newIndx = indx - 15;
                        incomingNumber = incomingNumber.substring(newIndx, indx);
                        indx = incomingNumber.indexOf("+");
                        if(indx>0){
                            incomingNumber = incomingNumber.substring(indx);
                            Log.d(DEBUG_TAG, "Mobile Number: " + incomingNumber);
                        }
                    }*/
                }
            }

            // Block this sms message from propagating to other sms applications
            abortBroadcast();

            // Send SMS to STStatus for debug
            /*Intent smsReceivedIntent = new Intent();
            smsReceivedIntent.setAction(ST_SMS_MESSAGE_RECEIVED);
            smsReceivedIntent.putExtra("smsCount", smsCount);
            smsReceivedIntent.putExtra("sender", sender);
            smsReceivedIntent.putExtra("message", message);
            sendBroadcast(smsReceivedIntent);
            Log.i("SMSTAG", "sms captured!");*/

            //TODO Fix This!
            // Send an auto-reply to the sender
            /*SmsManager smsManager = SmsManager.getDefault();
            String autoReply = getResources().getString(R.string.st_service_active_message);
            smsManager.sendTextMessage(messages[0].getOriginatingAddress(), null, autoReply, null, null);
            Log.i("SMSTAG", "sms auto-reply sent!");*/
        }
    };

    private final BroadcastReceiver smsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String sender = null;
            String message = null;
            SmsMessage[] messages = null;

            // Extract the SMS message from the PDU
            if (action.equals(SMS_RECEIVED_ACTION)) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < messages.length; i++) {
                        messages[i] = SmsMessage
                                .createFromPdu((byte[]) pdus[i]);
                        sender = messages[i].getOriginatingAddress();
                        message = messages[i].getMessageBody();
                        smsCount++;
                    }
                }
            }

            // Block this sms message from propagating to other sms applications
            abortBroadcast();

            // Send SMS to STStatus for debug
            Intent smsReceivedIntent = new Intent();
            smsReceivedIntent.setAction(ST_SMS_MESSAGE_RECEIVED);
            smsReceivedIntent.putExtra("smsCount", smsCount);
            smsReceivedIntent.putExtra("sender", sender);
            smsReceivedIntent.putExtra("message", message);
            sendBroadcast(smsReceivedIntent);
            Log.i("SMSTAG", "sms captured!");

            // Add message to the smsCache
            Object[] pdus = (Object[]) intent.getExtras().get("pdus");
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdus[0]);
            smsBuffer.add(sms);

            // Send an auto-reply to the sender
            SmsManager smsManager = SmsManager.getDefault();
            String autoReply = getResources().getString(R.string.st_service_active_message);
            smsManager.sendTextMessage(messages[0].getOriginatingAddress(), null, autoReply, null, null);
            Log.i("SMSTAG", "sms auto-reply sent!");
        }
    };

    public static void startSMSCapture(Context context) {
        Intent intent = new Intent(context, DMNCaptureService.class);
        intent.setAction(ST_START_CAPTURE);
        context.startService(intent);
    }

    public static void stopSMSCapture(Context context) {
        Intent intent = new Intent(context, DMNCaptureService.class);
        intent.setAction(ST_STOP_CAPTURE);
        context.startService(intent);
    }

    @Override
    public void onDestroy() {
        if (smsReceiverRegistered) {
            unregisterReceiver(smsReceiver);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
