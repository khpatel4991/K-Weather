package com.phone.khpatel4991.k_weather.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class WeatherProvider extends ContentProvider
{
    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;

    private static final UriMatcher _URIMATCHER = buildUriMatcher();
    private WeatherDbHelper _openHelper;
    private static final SQLiteQueryBuilder _WEATHER_BY_LOCATIONSETTING_QUERYBUILDER;
    static
    {
        _WEATHER_BY_LOCATIONSETTING_QUERYBUILDER = new SQLiteQueryBuilder();
        _WEATHER_BY_LOCATIONSETTING_QUERYBUILDER.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                        " = " + WeatherContract.LocationEntry.TABLE_NAME +
                        "." + WeatherContract.LocationEntry._ID);
    }

    private static final String _LOCATIONSETTING_SELECTION =
            WeatherContract.LocationEntry.TABLE_NAME+
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    private static final String _LOCATIONSETTING_WITH_STARTDATE_SELECTION =
            WeatherContract.LocationEntry.TABLE_NAME+
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATETEXT + " >= ? ";

    private static final String _LOCATIONSETTING_AND_DAY_SELECTION =
            WeatherContract.LocationEntry.TABLE_NAME +
                    "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                    WeatherContract.WeatherEntry.COLUMN_DATETEXT + " = ? ";

    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder)
    {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == null) {
            selection = _LOCATIONSETTING_SELECTION;
            selectionArgs = new String[]{locationSetting};
        } else {
            selectionArgs = new String[]{locationSetting, startDate};
            selection = _LOCATIONSETTING_WITH_STARTDATE_SELECTION;
        }

        return _WEATHER_BY_LOCATIONSETTING_QUERYBUILDER.query(_openHelper.getReadableDatabase(),
                                                           projection,
                                                           selection,
                                                           selectionArgs,
                                                           null,
                                                           null,
                                                           sortOrder
        );
    }

    private Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder)
    {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String date = WeatherContract.WeatherEntry.getDateFromUri(uri);

        return _WEATHER_BY_LOCATIONSETTING_QUERYBUILDER.query(_openHelper.getReadableDatabase(),
                                                           projection,
                                                           _LOCATIONSETTING_AND_DAY_SELECTION,
                                                           new String[]{locationSetting, date},
                                                           null,
                                                           null,
                                                           sortOrder
        );
    }

    private static UriMatcher buildUriMatcher()
    {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WeatherContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, WeatherContract.PATH_WEATHER, WEATHER);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_WEATHER + "/*/*", WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority, WeatherContract.PATH_LOCATION, LOCATION);
        matcher.addURI(authority, WeatherContract.PATH_LOCATION + "/#", LOCATION_ID);

        return matcher;
    }

    @Override
    public boolean onCreate()
    {
        _openHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {

        Cursor retCursor;
        switch (_URIMATCHER.match(uri))
        {
            // "weather/*/*"
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                retCursor = getWeatherByLocationSettingAndDate(uri, projection, sortOrder);
                break;
            }
            // "weather/*"
            case WEATHER_WITH_LOCATION:
            {
                retCursor = getWeatherByLocationSetting(uri, projection, sortOrder);
                break;
            }
            // "weather"
            case WEATHER:
            {
                retCursor = _openHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location/*"
            case LOCATION_ID:
            {
                retCursor = _openHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        WeatherContract.LocationEntry._ID + " = '" + ContentUris.parseId(uri) + "'",
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "location"
            case LOCATION:
            {
                retCursor = _openHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri)
    {
        final int match = _URIMATCHER.match(uri);

        switch (match)
        {
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues)
    {
        final SQLiteDatabase db = _openHelper.getWritableDatabase();
        final int match = _URIMATCHER.match(uri);
        Uri returnUri;

        switch(match)
        {
            case WEATHER:
            {
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, contentValues);
                if(_id > 0)
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into" + uri);
                break;
            }

            case LOCATION:
            {
                long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, contentValues);
                if(_id > 0)
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into" + uri);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectioneArgs)
    {
        final SQLiteDatabase db = _openHelper.getWritableDatabase();
        final int match = _URIMATCHER.match(uri);
        int rowsDeleted;

        switch(match)
        {
            case WEATHER:
            {
                rowsDeleted = db.delete(WeatherContract.WeatherEntry.TABLE_NAME, selection, selectioneArgs);
                break;
            }

            case LOCATION:
            {
                rowsDeleted = db.delete(WeatherContract.LocationEntry.TABLE_NAME, selection,
                                        selectioneArgs);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (selection == null || rowsDeleted != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs)
    {
        final SQLiteDatabase db = _openHelper.getWritableDatabase();
        final int match = _URIMATCHER.match(uri);
        int rowsUpdated;

        switch (match)
        {
            case WEATHER:
                rowsUpdated = db.update(WeatherContract.WeatherEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            case LOCATION:
                rowsUpdated =  db.update(WeatherContract.LocationEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values)
    {
        final SQLiteDatabase db = _openHelper.getWritableDatabase();
        final int match = _URIMATCHER.match(uri);
        switch (match)
        {
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values)
                    {
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally
                {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
