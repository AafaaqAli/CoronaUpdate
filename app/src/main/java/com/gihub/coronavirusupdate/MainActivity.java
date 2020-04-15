package com.gihub.coronavirusupdate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private static final int JOB_ID = 12;
    private static final int LOCATION_ID = 13;

    private SharedPreferences sharedPreferences;


    private int retryCounter = 0;
    private boolean skipMethod = false;

    private RequestPermissionsTask permissionsTask;
    private ImageView /*imageViewSearch,*/ imageViewSort;
    private ImageView imageViewWeatherToday;
    private TextView textViewTemperatureToday;
    private TextView textViewWeatherDetailToday;
    private Boolean isSortedInList = false;
    private RelativeLayout rootLayout;
    private HelperClass helperClass;
    private RecyclerView recyclerViewWeather;
    private RecyclerView recyclerViewCorona;

    private DataRepository dataRepository;
    private ProgressBar progressBar;

    private ArrayList<Corona> coronaArrayListFull;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataRepository = new DataRepository(getApplication());

        /*imageViewSearch = findViewById(R.id.imageViewSearchByCountry);*/
        imageViewSort = findViewById(R.id.imageViewSortCoronaPatients);
        rootLayout = findViewById(R.id.rootLayoutMain);

        recyclerViewWeather = findViewById(R.id.recyclerViewWeather);
        recyclerViewCorona = findViewById(R.id.recyclerViewCoronaPatients);

        sharedPreferences = getSharedPreferences("isFirstTime", MODE_PRIVATE);

        progressBar = findViewById(R.id.progressBar);

        imageViewWeatherToday = findViewById(R.id.imageViewWeatherMainLayout);
        textViewWeatherDetailToday = findViewById(R.id.textViewWeatherNameMain);
        textViewTemperatureToday = findViewById(R.id.textViewTemperatureMain);


        permissionsTask = new RequestPermissionsTask();


        //classes initialization here
        helperClass = new HelperClass();
        skipMethod = true;




        /* searchPatientsByCountry();*/
        sortPatients();
    }


    private void getLiveData() {
        if (helperClass.isNetworkWorking(this)) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                dataRepository.getAllLiveData().observe(this, data -> {
                    imageViewSort.setImageResource(R.drawable.ic_location);
                    int size = data.size() != 0 && data.size() < 5 ? data.size() : 5;
                    // Convert Database Model into RecyclerView Model
                    ArrayList<Weather> weatherArrayList = new ArrayList<>();
                    ArrayList<Corona> coronaArrayList = new ArrayList<>();

                    if (!data.isEmpty()) {
                        for (int x = 0; x < size; x++) {
                            weatherArrayList.add(
                                    new Weather(
                                            data.get(x).getId(),
                                            data.get(x).getTemperature(),
                                            data.get(x).getWindSpeed(),
                                            data.get(x).getWeatherDetail(),
                                            data.get(x).getWeatherDetail(),
                                            data.get(x).getHumidity(),
                                            data.get(x).getIconResource()
                                    ));

                            String currentDate = getCurrentDate();
                            for (int c = 0; c < data.size(); c++) {
                                coronaArrayList.add(new Corona(
                                        data.get(c).getCountryCode(),
                                        currentDate,
                                        data.get(c).getCountry(),
                                        data.get(c).getPatients(),
                                        data.get(c).getDeaths(),
                                        data.get(c).getRecovered(),
                                        data.get(c).getDeathWW(),
                                        data.get(c).getPatientsWW(),
                                        data.get(c).getRecoveredWW()));
                            }
                        }
                        // Update RecyclerView
                        setWeatherData(weatherArrayList);
                        setCoronaData(coronaArrayList);

                        coronaArrayListFull = new ArrayList<>(coronaArrayList);
                        imageViewSort.setVisibility(View.VISIBLE);

                    } else {
                        if (helperClass.isNetworkWorking(this)) {
                            startUpdate();

                        }
                    }
                });
            } else {
                requestLocationPermissions();
            }
        }
    }

    private void getLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsTask.execute();
            }
        }
    }

    private void sortPatients() {
        imageViewSort.setOnClickListener(v -> {
            String country = getApplicationContext().getResources().getConfiguration().locale.getDisplayCountry();
            ArrayList<Corona> coronaArrayList = new ArrayList<>();

            if (isSortedInList) {
                isSortedInList = false;
                if (!coronaArrayListFull.isEmpty()) {
                    setCoronaData(coronaArrayListFull);
                    imageViewSort.setImageResource(R.drawable.ic_location);
                } else {
                    getLiveData();
                    imageViewSort.setVisibility(View.GONE);
                }

            } else {
                isSortedInList = true;
                if (!coronaArrayListFull.isEmpty()) {
                    setCoronaData(null);
                    for (Corona corona : coronaArrayListFull) {
                        if (corona.getCountry().toLowerCase().equals(country.toLowerCase())) {
                            if (coronaArrayList.isEmpty()) {
                                coronaArrayList.add(corona);
                            } else {
                                for (Corona c : coronaArrayList) {
                                    if (c.getDeaths() != corona.getDeaths() && c.getPatients() != corona.getPatients()) {
                                        coronaArrayList.add(corona);
                                    }
                                }
                            }
                        }
                    }

                }
                setCoronaData(coronaArrayList);
                imageViewSort.setImageResource(R.drawable.ic_my_location);
            }
        });
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
                .setPositiveButton("Wifi", (dialog, id) -> {
                    dialog.dismiss();
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                })
                .setNegativeButton("Mobile Data", (dialog, id) -> {
                    dialog.dismiss();
                    startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
                })
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
                            d.dismiss();
                            startActivity(new Intent(action));
                        })
                .setNegativeButton("Dismiss", (d, id) -> d.cancel())
                .setCancelable(false);
        builder.create().show();
    }

    private void validateAndGPSAndNetworkSetting() {
        if (helperClass.isNetworkAvailable(this) && helperClass.isGPSActive(this)) {
            //GPS and Network is Available
            if (helperClass.isNetworkWorking(this)) {
                showSnack("Network available...", -1);
                getLocationPermission();
                getLiveData();
            } else {
                showSnack("Network available, But not working...", -1);
            }

        } else {
            //No GPS and Network Available
            showSnack("Network & GPS will help the application to update the weather and Corona patient count...  ", 0);

            if (helperClass.isGPSActive(this)) {
                showNetworkDialog();
            } else {
                showGPSDialog();
            }
        }
    }

    private void startUpdate() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (helperClass.isNetworkWorking(this)) {
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
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "ERROR, Networks not working...!", Toast.LENGTH_LONG).show();
            }
        }, 5000);
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
                    if (helperClass.isNetworkWorking(this)) {
                        showSnack("permissions granted...!", 1);
                    }

                } else {
                    showSnack("Weather update cannot work without location permission...!", 1);
                }
                return;
            }
        }
    }

    private int getImageResource(Weather weather) {
        int iconResource;

        try {
            String iconResourceString = weather.getIconResource();
            switch (iconResourceString) {
                case "01d":
                    iconResource = R.drawable.full_sun;
                    break;

                case "01n":
                    iconResource = R.drawable.night;
                    break;

                case "02d":
                    iconResource = R.drawable.day_cloudy_foggy;
                    break;

                case "02n":
                    iconResource = R.drawable.night_cloudy_foggy;
                    break;

                case "03d":
                    iconResource = R.drawable.clouds;
                    break;

                case "03n":
                    iconResource = R.drawable.clouds;
                    break;

                case "04n":
                    iconResource = R.drawable.clouds;
                    break;

                case "04d":
                    iconResource = R.drawable.clouds;
                    break;

                case "09d":
                    iconResource = R.drawable.cloud_rain;
                    break;

                case "09n":
                    iconResource = R.drawable.cloud_rain;
                    break;

                case "10d":
                    iconResource = R.drawable.day_cloud_rain;
                    break;

                case "10n":
                    iconResource = R.drawable.night_cloud_rain;
                    break;

                case "11d":
                    iconResource = R.drawable.thunder_storm_light_rainfall;
                    break;

                case "11n":
                    iconResource = R.drawable.thunder_storm_light_rainfall;
                    break;

                case "13d":
                    iconResource = R.drawable.heavy_snow;
                    break;

                case "13n":
                    iconResource = R.drawable.heavy_snow;
                    break;

                case "50d":
                    iconResource = R.drawable.mist_foggy;
                    break;

                case "50n":
                    iconResource = R.drawable.mist_foggy;
                    break;
                default:
                    iconResource = R.drawable.ic_clear;
            }
            return iconResource;

        } catch (NullPointerException e) {
            e.getMessage();
        }

        return R.drawable.ic_clear;

    }

    @SuppressLint("SetTextI18n")
    private void setTodayWeather(Weather weather) {
        Picasso.get().load(getImageResource(weather)).into(imageViewWeatherToday);
        textViewTemperatureToday.setText(helperClass.convertToC(weather.getTemperature()) + "\u2103");
        textViewWeatherDetailToday.setText(weather.getWeatherDetail());

    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        @SuppressLint("SimpleDateFormat") SimpleDateFormat outFormat = new SimpleDateFormat("EEEE MMM yyyy");
        return outFormat.format(new Date());
    }

    private void setWeatherData(ArrayList<Weather> list) {
        if (list.size() > 0) {
            progressBar.setVisibility(View.GONE);
            recyclerViewWeather.setAdapter(new WeatherAdapter(MainActivity.this, list));
            recyclerViewWeather.setLayoutManager(new GridLayoutManager(this, 1, RecyclerView.HORIZONTAL, false));
            setTodayWeather(list.get(0));
            retryCounter = 0;
        }
    }

    private void setCoronaData(@Nullable ArrayList<Corona> list) {
        if (list == null) {
            recyclerViewCorona.setAdapter(null);
        } else {
            if (list.size() > 0) {
                recyclerViewCorona.setAdapter(new CoronaAdapter(MainActivity.this, list));
                recyclerViewCorona.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        validateAndGPSAndNetworkSetting();
    }

    @SuppressLint("StaticFieldLeak")
    private class RequestPermissionsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            requestLocationPermissions();
            return null;
        }

    }
}
