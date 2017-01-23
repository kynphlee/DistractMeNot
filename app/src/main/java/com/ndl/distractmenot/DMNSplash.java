package com.ndl.distractmenot;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class DMNSplash extends AppCompatActivity {

    private final static String TAG = DMNSplash.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmnsplash);

        Log.d(TAG, "DistractMeNot Splash Screen");
        new SplashScreenTask().execute();
    }

    private class SplashScreenTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Intent initializer = new Intent(DMNSplash.this, DMNStatus.class);
            startActivity(initializer);
            finish();
        }
    }
}
