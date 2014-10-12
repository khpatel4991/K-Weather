package com.phone.khpatel4991.k_weather;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ShareActionProvider;
import android.widget.TextView;


public class DetailActivity extends Activity
{
    private ShareActionProvider _shareActionProvider;
    private String _forecastString;
    private final String LOG_TAG = DetailActivity.class.getSimpleName();
    private final String SHARE_SIGNATURE_HASHTAG = "\n-Shared via #K-Weather App";

    private Intent createForecastShareIntent()
    {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(shareIntent.EXTRA_TEXT, _forecastString + SHARE_SIGNATURE_HASHTAG);
        return shareIntent;
    }

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if(savedInstanceState == null)
        {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        MenuItem item = menu.findItem(R.id.menu_weather_share);
        _shareActionProvider = (ShareActionProvider) item.getActionProvider();
        if(_shareActionProvider != null)
        {
            Log.d(LOG_TAG, _forecastString + SHARE_SIGNATURE_HASHTAG);
            _shareActionProvider.setShareIntent(createForecastShareIntent());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.action_settings)
        {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment
    {
        public PlaceholderFragment()
        {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
            Intent intent = getActivity().getIntent();

            if(intent != null && intent.hasExtra(Intent.EXTRA_TEXT))
            {
                _forecastString = intent.getStringExtra(intent.EXTRA_TEXT);
                ((TextView)rootView.findViewById(R.id.detail_text)).setText(_forecastString);
            }

            return rootView;
        }

    }
}
