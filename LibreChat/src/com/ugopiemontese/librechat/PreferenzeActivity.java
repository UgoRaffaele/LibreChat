package com.ugopiemontese.librechat;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
//import android.support.v4.app.NavUtils;

public class PreferenzeActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferenze);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	// GESTORE ACTIONBAR //
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_preferenze, menu);
        return true;
    }
	
	// GESTORE MENU IN ACTIONBAR //
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                //NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
	
}
