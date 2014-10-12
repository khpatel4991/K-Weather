package com.phone.khpatel4991.k_weather;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.CursorAdapter;

import com.phone.khpatel4991.k_weather.data.WeatherContract;
import com.phone.khpatel4991.k_weather.data.WeatherContract.LocationEntry;
import com.phone.khpatel4991.k_weather.data.WeatherContract.WeatherEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class FetchWeatherTask extends AsyncTask<String, Void, Void>
{
    private final static String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private CursorAdapter _forecastAdapter;
    private final Context _context;
    private Utility _utility = new Utility();

    public FetchWeatherTask(Context context, CursorAdapter forecastAdapter)
    {
        _context = context;
        _forecastAdapter = forecastAdapter;
    }

    @Override
    protected Void doInBackground(String... params)
    {
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String responseFormat = "json";
        String units = "metric";
        Integer noOfDays = 14;

        String locationQuery = params[0];

        String forecastJsonStr = null;

        try
        {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are available at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast

            final String BASE_FORECAST_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String PARAM_Q = "q";
            final String PARAM_FORMAT = "mode";
            final String PARAM_UNITS = "units";
            final String PARAM_DAYS_COUNT = "cnt";

            Uri uriBuilder = Uri.parse(BASE_FORECAST_URL).buildUpon()
                    .appendQueryParameter(PARAM_Q, params[0])
                    .appendQueryParameter(PARAM_FORMAT, responseFormat)
                    .appendQueryParameter(PARAM_UNITS, units)
                    .appendQueryParameter(PARAM_DAYS_COUNT, Integer.toString(noOfDays))
                    .build();

            URL url = new URL(uriBuilder.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null)
            {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null)
            {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0)
            {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
        }
        catch (IOException e)
        {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        }
        finally
        {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (final IOException e)
                {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try
        {
            getWeatherDataFromJson(forecastJsonStr, noOfDays,  locationQuery);
        }
        catch(JSONException e)
        {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return null;
    }

    private Void getWeatherDataFromJson(String jsonString, Integer noOfDays, String locationQuery) throws JSONException
    {

        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DATETIME = "dt";
        final String OWM_DESCRIPTION = "main";
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_COORDINATES = "coord";
        final String OWM_LAITUDE = "lat";
        final String OWM_LONGITUDE = "lon";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WEATHER_ID = "id";
        final String OWM_WIND_SPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";


        JSONObject json = new JSONObject(jsonString);

        final String cityName = json.getJSONObject(OWM_CITY).getString(OWM_CITY_NAME);
        final double cityLatitude = json.getJSONObject(OWM_CITY).getJSONObject(OWM_COORDINATES).getDouble(OWM_LAITUDE);
        final double cityLongitude = json.getJSONObject(OWM_CITY).getJSONObject(OWM_COORDINATES).getDouble(OWM_LONGITUDE);
        long locationID = addLocation(cityName, locationQuery, cityLatitude, cityLongitude);

        JSONArray daysJsonArray = json.getJSONArray(OWM_LIST);

        Vector<ContentValues> cVVector = new Vector<ContentValues>(daysJsonArray.length());

        String[] result = new String[noOfDays];

        for(int i = 0 ; i < daysJsonArray.length() ; i++)
        {
            //Format of the string {day - description - hiLowStr}
            JSONObject dayJson = daysJsonArray.getJSONObject(i);

            //Main(List) JSON
            double pressure = dayJson.getDouble(OWM_PRESSURE);
            double humidity = dayJson.getInt(OWM_HUMIDITY);
            double windSpeed = dayJson.getDouble(OWM_WIND_SPEED);
            double windDirection = dayJson.getDouble(OWM_WIND_DIRECTION);
            long dateTime = dayJson.getLong(OWM_DATETIME);

            //Temp JSON
            double high = dayJson.getJSONObject(OWM_TEMPERATURE).getDouble(OWM_MAX);
            double low = dayJson.getJSONObject(OWM_TEMPERATURE).getDouble(OWM_MIN);

            //Weather JSON
            String weatherDescription = dayJson.getJSONArray(OWM_WEATHER).getJSONObject(0).getString(OWM_DESCRIPTION);
            int weatherId = dayJson.getJSONArray(OWM_WEATHER).getJSONObject(0).getInt(OWM_WEATHER_ID);

            //long locationID = addLocation(cityName, locationQuery, cityLatitude, cityLongitude);


            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationID);
            weatherValues.put(WeatherEntry.COLUMN_DATETEXT, WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
            weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, weatherDescription);
            weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);

            cVVector.add(weatherValues);

            String day = getReadableDateString(dayJson.getLong(OWM_DATETIME));
            String description = dayJson.getJSONArray(OWM_WEATHER).getJSONObject(0).getString(OWM_DESCRIPTION);
            String hiLowStr = formatHighLows(high, low);
            result[i] = day + " - " + description + " - " + hiLowStr;
        }

        if(cVVector.size() > 0)
            _context.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, cVVector.toArray(new ContentValues[cVVector.size()]));

        return null;
    }

    private String getReadableDateString(long time)
    {
        Date date = new Date(time * 1000);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date);
    }

    private String formatHighLows(double high, double low)
    {
        if(_utility.getUnitType(_context).equals(_context.getString(R.string.pref_units_imperial)))
        {
            high = high * (9 / 5) + 32;
            low = low * (9 / 5) + 32;
        }

        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        return roundedHigh + "/" + roundedLow;
    }

    private long addLocation(String locationSetting, String cityName, double lat, double lon)
    {
        Log.v(LOG_TAG, "inserting " + cityName + " with coordinates " + lat + " " + lon);
        Cursor cursor = _context.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                new String[]{LocationEntry._ID},
                LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if (cursor.moveToFirst())
        {
            Log.v(LOG_TAG, "Found it in the database!");
            int locationIdIndex = cursor.getColumnIndex(LocationEntry._ID);
            return cursor.getLong(locationIdIndex);
        }
        else
        {
            Log.v(LOG_TAG, "Didn't find it in the database, inserting now!");
            ContentValues locationValues = new ContentValues();
            locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(LocationEntry.COLUMN_LATITUDE, lat);
            locationValues.put(LocationEntry.COLUMN_LONGITUDE, lon);

            Uri locationInsertUri = _context.getContentResolver()
                    .insert(LocationEntry.CONTENT_URI, locationValues);

            return ContentUris.parseId(locationInsertUri);
        }
    }
}
