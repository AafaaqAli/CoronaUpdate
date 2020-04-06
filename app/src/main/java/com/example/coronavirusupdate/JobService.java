package com.example.coronavirusupdate;

import android.app.job.JobParameters;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.util.Log;

import com.amitshekhar.DebugDB;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.androidnetworking.AndroidNetworking;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class JobService extends android.app.job.JobService {
    private DataRepository dataRepository;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final String WEATHER_APP_ID = "&appid=38135a4ea014b16d241519186ab11d71";
    private static final String CORONA_VIRUS_URL = "https://coronavirus-tracker-api.herokuapp.com/v2/locations";
    double latitude, longitude;
    private String weatherApiURL = "api.openweathermap.org/data/2.5/forecast";
    ArrayList<Data> dataArrayList;
    ArrayList<Weather> weatherArrayList;
    ArrayList<WeatherDetails> weatherDetailsArrayList;
    int retryCounter = 0;
    int retryCounterWeather = 0;


    public static String getDate(long milliSeconds) {
        // Creating date format
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy HH:mm:ss:SSS Z");
        Date result = new Date(milliSeconds);

        return simple.format(result);
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private void getFusedLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.getLocationAvailability().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                    try {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        //should not be null
                        getWeather(location.getLatitude(), location.getLongitude());


                    } catch (NullPointerException e) {
                        getFusedLocation();

                    }
                });
            }
        });
    }

    private String getCityByLocation(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String country = "Unable to find The Country";
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            country = addresses.get(0).getAdminArea();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return country;
    }

    private void getWeather(double lat, double lon) {
        Log.d("locationTestService", "lat: " + lat + " lon: " + lon);
        String weatherAPIUrl = "https://" + weatherApiURL + "?lat=" + lat + "&lon=" + lon + WEATHER_APP_ID;
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST, weatherAPIUrl,
                null,
                response -> {
                    try {

                        //40 days json Array
                        JSONArray weatherJsonArray = response.getJSONArray("list");

                        for (int x = 0; x <= 6; x++) {
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
                        JSONArray jsonArray = response.getJSONArray("locations");
                        String confirmedWW = response.getJSONObject("latest").getString("confirmed");
                        String deathsWW = response.getJSONObject("latest").getString("deaths");
                        String recoveredWW = response.getJSONObject("latest").getString("recovered");

                        for (int x = 0; x <= jsonArray.length(); x++) {
                            if (weatherArrayList.size() != 0 && x < 5) {
                                dataArrayList.add(new Data(
                                        weatherArrayList.get(x).getWeatherDetail(),
                                        weatherArrayList.get(x).getTemperature(),
                                        weatherArrayList.get(x).getHumidity(),
                                        weatherArrayList.get(x).getIconResource(),
                                        weatherArrayList.get(x).getWindSpeed(),
                                        jsonArray.getJSONObject(x).get("country").toString(),
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


    private void performCoronaDatabaseOperations() {
        if (dataArrayList.isEmpty()) {
            if (retryCounter == 3) {
                Log.d("ServiceTestRun", "RetryCounter: " + retryCounter);
                return;
            } else {
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    performCoronaDatabaseOperations();
                    retryCounter++;
                }, 2000);
            }

        } else {
            retryCounter = 0;
            dataRepository = new DataRepository(getApplication());
            dataRepository.update(dataArrayList);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {

        dataArrayList = new ArrayList<>();
        weatherArrayList = new ArrayList<>();
        weatherDetailsArrayList = new ArrayList<>();

        DebugDB.getAddressLog();
        AndroidNetworking.initialize(this);
        getFusedLocation();

        getCoronaVirusUpdate();
        performCoronaDatabaseOperations();
        return true;
    }
}
