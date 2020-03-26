package com.example.coronavirusupdate;

import android.Manifest;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;


public class MainActivity extends AppCompatActivity {
    private static final int JOB_ID = 12;
    private static final int LOCATION_ID = 13;


    RequestPermissionsTask permissionsTask;
    ImageView imageViewSearch, imageViewSort;
    Boolean isSortedInList = true;
    RelativeLayout rootLayout;
    HelperClass helperClass;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewSearch = findViewById(R.id.imageViewSearchByCountry);
        imageViewSort = findViewById(R.id.imageViewSortCoronaPatients);
        rootLayout = findViewById(R.id.rootLayoutMain);

        permissionsTask = new RequestPermissionsTask();


        //classes initialization here
        helperClass = new HelperClass();

        //My Custom Functions
        sortPatients();
        searchPatientsByCountry();

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsTask.execute();
        }
    }

    private void sortPatients() {
        imageViewSort.setOnClickListener(v -> {
            if (isSortedInList) {
                isSortedInList = false;
                showSnack("Switched to List", -1);


            } else {
                isSortedInList = true;
                showSnack("Switched to Grid", -1);
            }
        });
    }

    private void searchPatientsByCountry() {
        imageViewSearch.setOnClickListener(v -> showSnack("Search Button pressed...", -1));
    }

    private void showSnack(String text, int snackLength) {
        Snackbar snackbar = Snackbar
                .make(rootLayout, text, snackLength);

        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.snackBarColor));
        snackbar.show();
    }

    private void showNetworkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("No Network Available, Want me to open Network setting?")
                .setCancelable(false)
                .setPositiveButton("Setting", (dialog, id) -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                .setNegativeButton("Dismiss", (dialog, id) -> dialog.dismiss())
                .setCancelable(false);


        AlertDialog alert = builder.create();
        alert.show();

    }

    private void showGPSDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
        final String message = "GPS is off, want me to open GPS setting?";

        builder.setMessage(message)
                .setPositiveButton("Setting",
                        (d, id) -> {
                            startActivity(new Intent(action));
                            d.dismiss();
                        })
                .setNegativeButton("Dismiss", (d, id) -> d.cancel())
                .setCancelable(false);
        builder.create().show();
    }

    private void validateAndGPSAndNetworkSetting() {
        if (helperClass.isNetworkAvailable(MainActivity.this) && helperClass.isGPSActive(MainActivity.this)) {
            showSnack("GPS & Network Available...", -1);
            if (helperClass.isGPSActive(MainActivity.this)) {
                startUpdate();
            }
        } else if (!helperClass.isNetworkAvailable(MainActivity.this) && helperClass.isGPSActive(MainActivity.this)) {
            showNetworkDialog();

        } else if (helperClass.isNetworkAvailable(MainActivity.this) && !helperClass.isGPSActive(MainActivity.this)) {
            showGPSDialog();
        } else {
            showNetworkDialog();
            showSnack("Network & GPS will help the application to update the weather and Corona patient count...  ", 0);

        }
    }


    private void startUpdate() {
        ComponentName componentName = new ComponentName(MainActivity.this, JobService.class);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(12 * 60 * 60 * 1000)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(jobInfo);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d("ServiceTestRun", "Job Scheduled Successfully...");

        }
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_ID);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_ID: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        startUpdate();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showSnack("Weather update cannot work without location permission...!", 1);
                }
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        validateAndGPSAndNetworkSetting();

    }

    private class RequestPermissionsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            requestLocationPermissions();
            return null;
        }
    }


}
