package com.coolhandsoftware.trailtrace;

import com.coolhandsoftware.topogen.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

/**
 * This is the activity launched from the "?" button. It describes how to use Trail Trace
 * and gives credit to the data sources used in the application.
 * @author David Cully
 * david.a.cully@gmail.com
 *
 */
public class HelpActivity extends Activity {

	protected void onCreate(Bundle sis) {
        super.onCreate(sis);
        setContentView(R.layout.help_activity);
        getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    // Respond to the action bar's Up/Home button
	    case android.R.id.home:
	        startActivity(new Intent(this, MapActivity.class));
	        return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
}
