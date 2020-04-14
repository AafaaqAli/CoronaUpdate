package com.gihub.coronavirusupdate;

import android.app.job.JobParameters;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.amitshekhar.DebugDB;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.androidnetworking.AndroidNetworking;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class JobService extends android.app.job.JobService {

    private static final String WEATHER_APP_ID = "&appid=38135a4ea014b16d241519186ab11d71";
    private static final String CORONA_VIRUS_URL = "https://coronavirus-tracker-api.herokuapp.com/v2/locations";

    private long UPDATE_INTERVAL = 60 * 60 * 1000;  /* 1 hour */
    private long FASTEST_INTERVAL = 60 * 60 * 1000; /* 1 hour */

    private LocationRequest mLocationRequest;


    private HelperClass helperClass;

    private double latitude, longitude;
    private int retryCounter = 0;

    private ArrayList<Data> dataArrayList;
    private ArrayList<Weather> weatherArrayList;
    private ArrayList<WeatherDetails> weatherDetailsArrayList;


    private static String getDate(long milliSeconds) {
        // Creating date format
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
        Date result = new Date(milliSeconds);

        return simple.format(result);
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private void getWeather(double lat, double lon) {
        String weatherApiURL = "api.openweathermap.org/data/2.5/forecast";
        String weatherAPIUrl = "https://" + weatherApiURL + "?lat=" + lat + "&lon=" + lon + WEATHER_APP_ID;

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, weatherAPIUrl,
                null,
                response -> {
                    try {

                        //40 days json Array
                        JSONArray weatherJsonArray = response.getJSONArray("list");
                        for (int x = 0; x < 5; x++) {
                            //Weather Each Day
                            JSONObject weatherSingleDayJsonObject = weatherJsonArray.getJSONObject(x);

                            //date of each day
                            String date = getDate(weatherSingleDayJsonObject.getLong("dt"));

                            //Weather Json Object Main
                            JSONObject weatherMainJsonObject = weatherSingleDayJsonObject.getJSONObject("main");

                            //get Weather stat of current city
                            String cityWeatherStat = weatherSingleDayJsonObject.getJSONArray("weather").toString();

                            //humidity
                            String humidity = weatherSingleDayJsonObject.getJSONObject("main").getString("humidity");

                            //city temperature
                            String cityTemperature = weatherMainJsonObject.getString("temp");


                            //windSpeed
                            String windSpeed = weatherSingleDayJsonObject.getJSONObject("wind").getString("speed");

                            //description
                            JSONArray jsonArrayWeather = weatherSingleDayJsonObject.getJSONArray("weather");
                            for (int i = 0; i < jsonArrayWeather.length(); i++) {
                                weatherDetailsArrayList.add(new WeatherDetails(
                                        jsonArrayWeather.getJSONObject(i).getString("id"),
                                        jsonArrayWeather.getJSONObject(i).getString("main"),
                                        jsonArrayWeather.getJSONObject(i).getString("description"),
                                        jsonArrayWeather.getJSONObject(i).getString("icon")
                                ));
                            }

                            weatherArrayList.add(new Weather(
                                    Integer.parseInt(weatherDetailsArrayList.get(x).getId()),
                                    cityTemperature,
                                    windSpeed,
                                    weatherDetailsArrayList.get(x).getDescription(),
                                    weatherDetailsArrayList.get(x).getMain(),
                                    humidity,
                                    weatherDetailsArrayList.get(x).getIcon()
                            ));

                        }


                        getCoronaVirusUpdate();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {

                }
        );

        // Add JsonObjectRequest to the RequestQueue
        requestQueue.add(jsonObjectRequest);
    }

    private void getCoronaVirusUpdate() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                CORONA_VIRUS_URL,
                null,
                response -> {
                    try {
                        Log.d("ServiceTestRun", "Corona Updated");
                        JSONArray jsonArray = response.getJSONArray("locations");
                        String confirmedWW = response.getJSONObject("latest").getString("confirmed");
                        String deathsWW = response.getJSONObject("latest").getString("deaths");
                        String recoveredWW = response.getJSONObject("latest").getString("recovered");

                        for (int x = 0; x <= jsonArray.length(); x++) {
                            String drawableName = jsonArray.getJSONObject(x).get("country_code").toString().toLowerCase();
                            int countryResID = getResources().getIdentifier(drawableName, "drawable", getPackageName());

                            if (weatherArrayList.size() != 0 && x < 5) {
                                dataArrayList.add(new Data(
                                        weatherArrayList.get(x).getWeatherDetail(),
                                        weatherArrayList.get(x).getTemperature(),
                                        weatherArrayList.get(x).getHumidity(),
                                        weatherArrayList.get(x).getIconResource(),
                                        weatherArrayList.get(x).getWindSpeed(),
                                        jsonArray.getJSONObject(x).get("country").toString(),
                                        countryResID,
                                        jsonArray.getJSONObject(x).getJSONObject("latest").get("confirmed").toString(),
                                        jsonArray.getJSONObject(x).getJSONObject("latest").get("deaths").toString(),
                                        jsonArray.getJSONObject(x).getJSONObject("latest").get("recovered").toString(),
                                        deathsWW,
                                        confirmedWW,
                                        recoveredWW
                                ));

                            } else {
                                dataArrayList.add(new Data(
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        jsonArray.getJSONObject(x).get("country").toString(),
                                        countryResID,
                                        jsonArray.getJSONObject(x).getJSONObject("latest").get("confirmed").toString(),
                                        jsonArray.getJSONObject(x).getJSONObject("latest").get("deaths").toString(),
                                        jsonArray.getJSONObject(x).getJSONObject("latest").get("recovered").toString(),
                                        deathsWW,
                                        confirmedWW,
                                        recoveredWW
                                ));
                            }

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("ServiceTestRun", e.getMessage());
                    }
                },
                error -> {
                    Log.d("ServiceTestRun", "Error: " + error.toString());
                }
        );


        // Add JsonObjectRequest to the RequestQueue
        requestQueue.add(jsonObjectRequest);

    }


    private void performDatabaseOperations() {
        if (dataArrayList.isEmpty()) {

            Handler handler = new Handler();
            handler.postDelayed(() -> {

                if (retryCounter == 5) {
                    return;
                } else {
                    restartJob();
                    retryCounter++;
                }

            }, 10000);

        } else {
            retryCounter = 0;
            DataRepository dataRepository = new DataRepository(getApplication());
            dataRepository.update(dataArrayList);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        helperClass = new HelperClass();
        dataArrayList = new ArrayList<>();
        weatherArrayList = new ArrayList<>();
        weatherDetailsArrayList = new ArrayList<>();

        AndroidNetworking.initialize(this);
        DebugDB.getAddressLog();
        startLocationUpdates();
        performDatabaseOperations();
        return true;
    }

    private void restartJob() {
        if (dataArrayList.isEmpty()) {
            startLocationUpdates();
        } else {
            performDatabaseOperations();
        }
    }

    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },

                Looper.myLooper());
    }

    public void onLocationChanged(Location location) {
        getWeather(location.getLatitude(), location.getLongitude());

    }
}
