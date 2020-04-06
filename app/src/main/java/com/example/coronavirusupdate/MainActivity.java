package com.example.coronavirusupdate;

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
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.data.DataBufferObserver;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity /*implements Observer */{
    private static final int JOB_ID = 12;
    private static final int LOCATION_ID = 13;

    SharedPreferences sharedPreferences;

    private int retryCounter = 0;

    RequestPermissionsTask permissionsTask;
    ImageView imageViewSearch, imageViewSort, imageViewWeatherToday;
    TextView textViewTemperatureToday, textViewWeatherDetailToday;
    Boolean isSortedInList = true;
    RelativeLayout rootLayout;
    HelperClass helperClass;
    RecyclerView recyclerViewWeather;
    DataRepository dataRepository;
    ProgressBar progressBar;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        dataRepository = new DataRepository(getApplication());

        imageViewSearch = findViewById(R.id.imageViewSearchByCountry);
        imageViewSort = findViewById(R.id.imageViewSortCoronaPatients);
        rootLayout = findViewById(R.id.rootLayoutMain);
        recyclerViewWeather = findViewById(R.id.recyclerViewWeather);
        sharedPreferences = getSharedPreferences("isFirstTime", MODE_PRIVATE);

        progressBar = findViewById(R.id.progressBar);

        imageViewWeatherToday = findViewById(R.id.imageViewWeatherMainLayout);
        textViewWeatherDetailToday = findViewById(R.id.textViewWeatherNameMain);
        textViewTemperatureToday = findViewById(R.id.textViewTemperatureMain);


        permissionsTask = new RequestPermissionsTask();


        //classes initialization here
        helperClass = new HelperClass();

        //My Custom Functions
        sortPatients();
        searchPatientsByCountry();
        getLocationPermission();


        dataRepository.getAllLiveData().observe(this, data -> {
            Log.d("ServiceTestRun", "Size of array is: " + data.size());

            int size = data.size() < 5 ? data.size() : 5;
            // Convert Database Model into RecyclerView Model
            ArrayList<Weather> weatherArrayList = new ArrayList<>();
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
            }

            // Update RecyclerView
            if(weatherArrayList.size() == 0){
                startUpdate();
            }else{
                setData(weatherArrayList);
            }

        });

    }

    private void getLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsTask.execute();
            }else{
                validateAndGPSAndNetworkSetting();
            }
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
        if (helperClass.isNetworkWorking(MainActivity.this)) {
            showSnack("GPS & Network Available...", -1);
            startUpdate();
            if (helperClass.isNetworkWorking(this)) {
                showSnack("Network available...", -1);
            } else {
                showSnack("Network is not working...", -1);
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


    @Override
    protected void onResume() {
        super.onResume();
        if ((!helperClass.isGPSActive(this) || !helperClass.isNetworkAvailable(this)) ||
                (!helperClass.isGPSActive(this) && !helperClass.isNetworkAvailable(this))) {
            getLocationPermission();
        }
    }

    private class RequestPermissionsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            requestLocationPermissions();
            return null;
        }
    }

    private Weather getTodayWeather(ArrayList<Weather> list) {
        if(list == null || list.size() <= 0) return null;
        return list.get(0);
    }

    private int getImageResource(String iconResourceString) {
        int iconResource = 0;
        if(iconResourceString != null){
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
                    iconResource = R.drawable.clouds_foggy;
                    break;

                case "03n":
                    iconResource = R.drawable.clouds_foggy;
                    break;

                case "04n":
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
            }

        }

        return iconResource;
    }

    @SuppressLint("SetTextI18n")
    private void setTodayWeather(Weather weather) {

        if (weather != null) {
            Picasso.get().load(getImageResource(weather.getIconResource())).into(imageViewWeatherToday);
            textViewTemperatureToday.setText(helperClass.convertToC(weather.getTemperature()) + "\u2103");
            textViewWeatherDetailToday.setText(weather.getWeatherDetail());
        }
    }



    private void setData(ArrayList<Weather> list) {
        progressBar.setVisibility(View.GONE);
        recyclerViewWeather.setAdapter(new WeatherAdapter(MainActivity.this, list));
        recyclerViewWeather.setLayoutManager(new GridLayoutManager(this, 1, RecyclerView.HORIZONTAL, false));
        Weather weather = getTodayWeather(list);
        setTodayWeather(weather);
        retryCounter = 0;
    }
}
