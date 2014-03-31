package com.photointeering;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;

import com.photointeering.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends FragmentActivity implements 
						GooglePlayServicesClient.ConnectionCallbacks, 
						GooglePlayServicesClient.OnConnectionFailedListener {

	public final static String GPS_COORDS = "com.photointeering.COORDS";

	private static final String DIALOG_ERROR = "dialog_error";
	
	Button newGameButton;
	Button joinGameButton;
	LocationClient mLocationClient;
	Location mCurrentLocation;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		newGameButton = (Button) findViewById(R.id.newGameButton);
		
		newGameButton.setOnClickListener(newGameButtonListener);
		
		joinGameButton = (Button) findViewById(R.id.joinGameButton);
		
		joinGameButton.setOnClickListener(joinGameButtonListener);
		
		mLocationClient = new LocationClient(this, this, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public OnClickListener newGameButtonListener = new OnClickListener() {
		public void onClick(View v) {
			mCurrentLocation = mLocationClient.getLastLocation();
			
			double lat = 0.0;
			double lon = 0.0;
			
			if (mCurrentLocation != null) {
				Log.d("location", mCurrentLocation.toString());
				lat = mCurrentLocation.getLatitude();
				lon = mCurrentLocation.getLongitude();
			}
			
			Intent intent = new Intent(MainActivity.this, GameMapActivity.class);
			intent.putExtra("lat", lat);
			intent.putExtra("lon", lon);
			
			Log.d("tag", "click! " + lat + " " + lon);
			
			startActivity(intent);
		}
		
	};
	
	public OnClickListener joinGameButtonListener = new OnClickListener() {
		public void onClick(View v) {
			mCurrentLocation = mLocationClient.getLastLocation();
			
			double lat = 0.0;
			double lon = 0.0;
			
			if (mCurrentLocation != null) {
				Log.d("location", mCurrentLocation.toString());
				lat = mCurrentLocation.getLatitude();
				lon = mCurrentLocation.getLongitude();
			}
			
			Intent intent = new Intent(MainActivity.this, JoinGameActivity.class);
			intent.putExtra("lat", lat);
			intent.putExtra("lon", lon);
			
			Log.d("tag", "click! " + lat + " " + lon);
			
			startActivity(intent);
		}
		
	};
	
	protected void onStart() {
		super.onStart();
		if (this.servicesConnected()) {
			mLocationClient.connect();
		}
	}

	// Everything involved with getting the current location
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	public static class ErrorDialogFragment extends DialogFragment {
		// Global field to contain the error dialog
		private Dialog mDialog;

		// Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		// Set the dialog to display
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		// Return a Dialog to the DialogFragment.
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	/*
	 * Handle results returned to the FragmentActivity by Google Play services
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Decide what to do based on the original request code
		switch (requestCode) {
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			/*
			 * If the result code is Activity.RESULT_OK, try to connect again
			 */
			switch (resultCode) {
			case Activity.RESULT_OK:
				/*
				 * Try the request again
				 */
				break;
			}
		}
	}

	private boolean servicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates", "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason
		} else {
			// Get the error code
			int errorCode = resultCode; //connectionResult.getErrorCode();
			// Get the error dialog from Google Play services
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				// Set the dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(getSupportFragmentManager(),
						"Location Updates");
			}
			return false;
		}
	}
//
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						CONNECTION_FAILURE_RESOLUTION_REQUEST);
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */
			} catch (IntentSender.SendIntentException e) {
				// Log the error
				e.printStackTrace();
			}
		} else {
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			showErrorDialog(connectionResult.getErrorCode());
		}
	}
	
	private void showErrorDialog(int errorCode) {
		// Create a fragment for the error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		// Pass the error that should be displayed
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getSupportFragmentManager(), "errordialog");
	}
//
	public void onConnected(Bundle dataBundle) {
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();

	}
//
	public void onDisconnected() {
		Toast.makeText(this, "Disconnected. Please re-connect.",
				Toast.LENGTH_SHORT).show();
	}
	
	
	protected void onStop() {
		mLocationClient.disconnect();
		super.onStop();
	}

}

