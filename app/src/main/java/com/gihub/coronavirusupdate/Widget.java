package com.gihub.coronavirusupdate;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

/**
 * Implementation of App Widget functionality.
 */
public class Widget extends AppWidgetProvider {
    ImageView imageViewWeather;
    TextView textViewTemperature;
    TextView textViewWeatherDescription;
    TextView textViewPresentCountry;
    TextView textViewPresentCountryPatients;
    TextView textViewWorldWidePatients;

    public Widget() {
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId) {


        int iconResource = R.drawable.ic_error_outline_black_24dp;
        String temperature = "90";
        String weatherDescription = "Error";
        String currentCountryLocation = "Pakistan";
        String currentCountryPatients = "0";
        String worldWidePatients = "0";


        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        //for weather Icon
        views.setImageViewResource(R.id.imageViewCardWeather, iconResource);

        // for temperature
        views.setTextViewText(R.id.textViewCardTemperature, temperature);

        //weather Description
        views.setTextViewText(R.id.textViewCardWeather, weatherDescription);

        //for current Country
        views.setTextViewText(R.id.textViewCurrentCountryTitle, currentCountryLocation);

        //for current country patients
        views.setTextViewText(R.id.textViewCurrentCountryConfirmed, currentCountryPatients);

        //for world wide patients
        views.setTextViewText(R.id.textViewWorldWidePatients, worldWidePatients);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

}

