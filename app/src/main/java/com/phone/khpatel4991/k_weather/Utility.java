package com.phone.khpatel4991.k_weather;

import android.content.Context;
import android.preference.PreferenceManager;

public class Utility
{
    public static String getPreferredLocation(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_location_key), context.getString(R.string.pref_location_default));
    }

    public static String getUnitType(Context context)
    {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.pref_units_key), context.getString(R.string.pref_units_metric));
    }
}
