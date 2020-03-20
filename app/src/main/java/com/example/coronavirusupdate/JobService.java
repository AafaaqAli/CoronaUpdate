package com.example.coronavirusupdate;

import android.app.job.JobParameters;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;

import java.util.Objects;

public class JobService extends android.app.job.JobService {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final String WEATHER_APP_ID = "&appid=38135a4ea014b16d241519186ab11d71";
    private static final String CORONA_VIRUS_URL = "        https://coronavirus-tracker-api.herokuapp.com/all";
    double latitude, longitude;
    private String weatherApiURL = "api.openweathermap.org/data/2.5/forecast";

    @Override
    public boolean onStartJob(JobParameters params) {
        AndroidNetworking.initialize(getApplicationContext());
        getFusedLocation(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


    private void getFusedLocation(JobParameters prams) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLocationAvailability().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                    try {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.d("ServiceTestRun", "Lat: " + latitude + "\n" + "lon: " + longitude);
                        getWeather(location.getLatitude(), location.getLongitude());
                        getCoronaVirusUpdate();

                    } catch (NullPointerException e) {
                        Log.d("ServiceTestRun", Objects.requireNonNull(e.getMessage()));
                    }
                });
            }
        });
    }

    private void getWeather(double lat, double lon) {
        String url = "https://" + weatherApiURL + "?lat=" + lat + "&lon=" + lon + WEATHER_APP_ID;
        AndroidNetworking.get(url)
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d("ServiceTestRun", response.toString());
                    }

                    @Override
                    public void onError(ANError error) {
                        Log.d("ServiceTestRun", error.toString());
                    }
                });

    }

    private void getCoronaVirusUpdate() {
        AndroidNetworking.get(CORONA_VIRUS_URL)
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONArray(new JSONArrayRequestListener() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d("ServiceTestRun", response.toString());
                    }

                    @Override
                    public void onError(ANError error) {
                        Log.d("ServiceTestRun", error.toString());
                    }
                });
    }

    private void updateWidget() {

    }

}
