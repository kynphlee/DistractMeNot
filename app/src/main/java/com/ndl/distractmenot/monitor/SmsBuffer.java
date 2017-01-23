package com.ndl.distractmenot.monitor;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.telephony.SmsMessage;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kynphlee on 1/19/17.
 */
public class SmsBuffer extends HashMap<String, ArrayList<SmsMessage>> {

    private static final long serialVersionUID = 1L;
    private Context mContext;

    public SmsBuffer(final Context context) {
        mContext = context;
    }

    public void add(SmsMessage smsMessage) {
        if (!containsKey(smsMessage.getOriginatingAddress())) {
            ArrayList<SmsMessage> smsList =  new ArrayList<SmsMessage>();
            smsList.add(smsMessage);
            put(smsMessage.getOriginatingAddress(), smsList);
        } else {
            ArrayList<SmsMessage> smsList = get(smsMessage.getOriginatingAddress());
            smsList.add(smsMessage);
            put(smsMessage.getOriginatingAddress(), smsList);
        }
    }

    public int count() {
        int size = 0;
        for(ArrayList<SmsMessage> senders: this.values()) {
            size += senders.size();
        }
        return size;
    }

    public int dumpSmsMessages() {
        int mCount = 0;
        for(ArrayList<SmsMessage> senders: this.values()) {
            for(SmsMessage message: senders) {
                final ContentValues values = new ContentValues();
                values.put("address", message.getOriginatingAddress());
                values.put("body", message.getMessageBody().toString());
                mContext.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
                mCount++;
            }
        }

        return mCount;
    }
}

