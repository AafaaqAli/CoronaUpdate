package com.example.coronavirusupdate;

import android.app.job.JobParameters;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class JobService extends android.app.job.JobService {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final String WEATHER_APP_ID = "&appid=38135a4ea014b16d241519186ab11d71";
    private static final String CORONA_VIRUS_URL = "https://coronavirus-tracker-api.herokuapp.com/all";
    double latitude, longitude;
    private String weatherApiURL = "api.openweathermap.org/data/2.5/forecast";
    ArrayList<Weather> arrayListWeather;
    ArrayList<Corona> arrayListCorona;


    public static String getDate(long milliSeconds) {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/y yyy");

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());

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
                        if (location == null) {
                            getFusedLocation();
                        } else {
                            Log.d("ServiceTestRun", "Lat: " + latitude + "\n" + "lon: " + longitude);
                            getWeather(location.getLatitude(), location.getLongitude());
                            getCoronaVirusUpdate();
                        }

                    } catch (NullPointerException e) {
                        Log.d("ServiceTestRun", Objects.requireNonNull(e.getMessage()));
                    }
                });
            }
        });
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        arrayListWeather = new ArrayList<>();
        arrayListCorona = new ArrayList<>();
        AndroidNetworking.initialize(this);
        getFusedLocation();

        return true;
    }

    private void getWeather(double lat, double lon) {
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

                            //to get Current Weather of city
                            JSONObject weatherJsonArrayCurrentCityStats = response.getJSONObject("city");

                            //get City Name
                            String cityName = weatherJsonArrayCurrentCityStats.getString("name");

                            //city temperature
                            String cityTemperature = weatherMainJsonObject.getString("temp");

                            //descriptionCityTemperature
                            String descriptionCityTemperature = weatherSingleDayJsonObject.getJSONArray("weather").toString();

                            arrayListWeather.add(new Weather(date, cityName, Double.parseDouble(cityTemperature), cityWeatherStat, descriptionCityTemperature));
                            arrayListCorona.add(new Corona(String.valueOf(x), "1000", "1000"));

                            Log.d("ServiceTestRun", "\nDate: " + date
                                    + " Temperature of City: " + cityName + " is " + cityTemperature + " and weather is: " + descriptionCityTemperature
                            );


                        }
                        DebugDB.getAddressLog();
                        WeatherRepository weatherRepository = new WeatherRepository(this);
                        weatherRepository.updateWeather(arrayListWeather);


                        CoronaRepository coronaRepository = new CoronaRepository(this);
                        coronaRepository.updateCorona(arrayListCorona);


                        Log.d("ServiceTetRun", "===============================================In Main=====================================================================");
                        for (Weather weather : weatherRepository.getAllWeather()) {
                            Log.d("ServiceTestRun", "\nDate: " + weather.getDate()
                                    + " Temperature of City: " + weather.getCity() + " is " + weather.getTemperature() + " and weather is: " + weather.getWeatherCondition()
                            );
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

    private void updateWidget() {

    }

    private void getCoronaVirusUpdate() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                CORONA_VIRUS_URL,
                null,
                response -> {

                    try {
                        String wwCases = response.getJSONObject("confirmed").get("latest").toString();
                        String totalCases = response.getJSONArray("locations").toString();
                        Log.d("ServiceTestRun", "WW cases are: " + wwCases + "\n" + totalCases);


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

    private void tailorWeatherData() {

    }
}
