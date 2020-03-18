package com.example.coronavirusupdate;

import android.app.job.JobParameters;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class JobService extends android.app.job.JobService {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private double latitude, longitude;
    BackgroundTask backgroundTask;
    @Override
    public boolean onStartJob(JobParameters params) {
        backgroundTask = new BackgroundTask();
        backgroundTask.execute();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


    private void getFusedLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        });

    }

    private void getWeather() {

    }

    private void getCoronaVirusUpdate() {

    }

    private void updateWidget(){

    }

    private class BackgroundTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            getFusedLocation();

            Log.d("ServiceTestRun", "Latitude is: " + latitude);
            Log.d("ServiceTestRun", "Longitude is: " + longitude);

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
