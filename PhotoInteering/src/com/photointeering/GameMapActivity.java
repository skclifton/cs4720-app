package com.photointeering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.photointeering.JoinGameActivity.JoinClickListener;
import com.photointeering.MainActivity.ErrorDialogFragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GameMapActivity extends FragmentActivity implements
		OnMarkerClickListener, GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	int IMAGE_WIDTH = 256;
	int IMAGE_HEIGHT = 180;

	double PHOTO_FOUND_DISTANCE = 0.005;

	private static final String TAG = "PHOTO";

	ImageView image;
	TextView latitudeTextView;
	TextView longitudeTextView;
	Button foundItButton;
	ArrayList<Double> ret = new ArrayList<Double>();
	ArrayList<Drawable> drawRet = new ArrayList<Drawable>();
	ArrayList<String> playersAndScores = new ArrayList<String>();
	ArrayList<Marker> unfoundMapMarkers = new ArrayList<Marker>();
	ArrayList<Marker> foundMapMarkers = new ArrayList<Marker>();
	private static final String DIALOG_ERROR = "dialog_error";

	static final String KEY_PHOTO_URL = "url";
	static final String KEY_PHOTO_LAT = "lat";
	static final String KEY_PHOTO_LON = "lon";

	String photoURL = "";
	double latitude = 0.0;
	double longitude = 0.0;

	String newGameURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/new_game/";
	String joinGameURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/join_game/";
	String getScoresURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/get_scores/";
	String recentGameURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/get_recent_game/";
	String updateScoreURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/update_score/";

	private TableLayout gamePlayersScrollView;

	// private TableLayout photoScrollView;

	GoogleMap map;
	private View infoWindow;
	public HashMap images = new HashMap<Marker, Bitmap>();
	LocationClient mLocationClient;
	Location mCurrentLocation;
	int gameID;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);

		// Initialize UI elements
		gamePlayersScrollView = (TableLayout) findViewById(R.id.gamePlayersTableLayout);
		Intent intent = getIntent();
		image = (ImageView) findViewById(R.id.image);
		latitudeTextView = (TextView) findViewById(R.id.latitudeTextView);
		longitudeTextView = (TextView) findViewById(R.id.longitudeTextView);
		foundItButton = (Button) findViewById(R.id.foundItButton);
		foundItButton.setOnClickListener(foundItButtonListener);

		mLocationClient = new LocationClient(this, this, this);

		Double currentLatDouble = intent.getDoubleExtra("lat", 0.0);
		Double currentLonDouble = intent.getDoubleExtra("lon", 0.0);

		String currentLat = Double.toString(currentLatDouble);
		String currentLon = Double.toString(currentLonDouble);
		String gpsCoords = intent.getStringExtra(MainActivity.GPS_COORDS);
		boolean newGame = intent.getBooleanExtra("newGame", true);

		Log.d("gameID after getting from getGameIDTask or from intent",
				String.valueOf(gameID));

		// Either begin a new game or join an existing game
		String sendURL = "";

		if (newGame) {
			sendURL = newGameURL + currentLat + "/" + currentLon + "/"
					+ getAccountName();
		} else {
			gameID = intent.getIntExtra("gameID", 0);
			sendURL = joinGameURL + gameID + "/" + getAccountName();
		}

		new MyAsyncTask().execute(sendURL);

		// If a new game was started, we must wait to get the game ID so that it
		// is the most recent
		if (newGame) {
			gameID = 0;
			getGameIDTask getGameID = new getGameIDTask();
			try {
				gameID = Integer.parseInt(getGameID.execute().get());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		infoWindow = getLayoutInflater().inflate(R.layout.custom_info_window,
				null);

		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();
		map.setOnMarkerClickListener(this);

		LatLng current = new LatLng(currentLatDouble, currentLonDouble);
		Marker currentMark = map.addMarker(new MarkerOptions()
				.position(current)
				.icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

		map.setInfoWindowAdapter(new CustomInfoAdapter());

		map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 0));

		// Zoom in, animating the camera.
		map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

		TextView gameIDTV = (TextView) findViewById(R.id.gameActivityGameIDTextView);
		gameIDTV.setText(String.valueOf(gameID));

		TextView player = (TextView) findViewById(R.id.playerTextView);
		player.setText(getAccountName());

		TextView photosFound = (TextView) findViewById(R.id.photosFoundTextView);
		photosFound.setText("0");

		getPlayers(gameID, newGame);

	}

	public OnClickListener foundItButtonListener = new OnClickListener() {
		public void onClick(View v) {

			mCurrentLocation = mLocationClient.getLastLocation();

			double lat = 0.0;
			double lon = 0.0;

			if (mCurrentLocation != null) {
				Log.d("location", mCurrentLocation.toString());
				lat = mCurrentLocation.getLatitude();
				lon = mCurrentLocation.getLongitude();
			}

			boolean displayWindow = true; // display the dialog box telling a
											// user they didn't find the point
			ArrayList<Marker> markersToRemove = new ArrayList<Marker>();
			
			for (Marker m : unfoundMapMarkers) {
				double photoLat = m.getPosition().latitude;
				double photoLon = m.getPosition().longitude;
				boolean found = Math.abs(photoLat - lat) < PHOTO_FOUND_DISTANCE
						&& Math.abs(photoLon - lon) < PHOTO_FOUND_DISTANCE;
				if (found) {
					TextView photosFound = (TextView) findViewById(R.id.photosFoundTextView);
					int score = Integer.parseInt(photosFound.getText()
							.toString());
					score += 1;
					photosFound.setText(String.valueOf(score));
					String sendURL = updateScoreURL + gameID + "/"
							+ getAccountName() + "/" + score;
					Log.d("gameID when updating score", String.valueOf(gameID));

					m.setIcon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
					
					markersToRemove.add(m);
					
					new UpdateScoreTask().execute(sendURL);
					displayWindow = false;
				}
			}
			
			for (Marker m : markersToRemove) {
				foundMapMarkers.add(m);
				unfoundMapMarkers.remove(m);
			}
			
			if (displayWindow) {
				PointNotFoundDialogFragment p = new PointNotFoundDialogFragment();
				p.show((GameMapActivity.this).getSupportFragmentManager(),
						"point not found dialog winidow");
			}
		}
	};

	private class UpdateScoreTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... args) {

			String sendURL = args[0];

			DefaultHttpClient httpclient = new DefaultHttpClient(
					new BasicHttpParams());

			HttpPost httppost = new HttpPost(sendURL);
			httppost.setHeader("Content-type", "application/json");

			InputStream inputStream = null;

			String result = null;

			try {
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				inputStream = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inputStream, "UTF-8"), 8);
				StringBuilder sb = new StringBuilder();

				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				result = sb.toString();
				Log.d("result", result);
			} catch (Exception e) {

			} finally {
				try {
					if (inputStream != null)
						inputStream.close();
				} catch (Exception squish) {
				}
			}
			return null;
		}

	}

	public void getPlayers(int gameID, boolean isNew) {

		String sendURL = "";
		String newGameURL = "";
		if (isNew) {
			newGameURL = recentGameURL + getAccountName();
		} else {
			sendURL = getScoresURL + gameID;
		}
		new MyUpdatePlayersTask().execute(newGameURL, sendURL);
	}

	class CustomInfoAdapter implements InfoWindowAdapter {

		@Override
		public View getInfoContents(Marker m) {
			displayView(m);
			return infoWindow;
		}

		@Override
		public View getInfoWindow(Marker m) {
			return null;
		}

	}

	public void displayView(Marker m) {
		Bitmap b = (Bitmap) images.get(m);
		((ImageView) infoWindow.findViewById(R.id.infoWindowImageView))
				.setImageBitmap(b);
	}

	@Override
	public boolean onMarkerClick(Marker m) {
		return false;
	}

	private String getAccountName() {
		AccountManager manager = AccountManager.get(this);
		Account[] accounts = manager.getAccountsByType("com.google");
		List<String> possibleEmails = new LinkedList<String>();

		for (Account account : accounts) {
			// TODO: Check possibleEmail against an email regex or treat
			// account.name as an email address only for certain account.type
			// values.
			possibleEmails.add(account.name);
		}

		if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
			String email = possibleEmails.get(0);
			String[] parts = email.split("@");
			if (parts.length > 0 && parts[0] != null)
				return parts[0];
			else
				return null;
		} else
			return null;
	}

	private class getGameIDTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... args) {

			String SendURL1 = recentGameURL + getAccountName();
			if (!SendURL1.equals("")) {
				DefaultHttpClient httpclient = new DefaultHttpClient(
						new BasicHttpParams());
				HttpPost httppost = new HttpPost(SendURL1);
				httppost.setHeader("Content-type", "application/json");
				InputStream inputStream = null;
				String result = null;

				try {
					HttpResponse response = httpclient.execute(httppost);
					HttpEntity entity = response.getEntity();
					inputStream = entity.getContent();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(inputStream, "UTF-8"), 8);
					StringBuilder sb = new StringBuilder();

					String line = null;
					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
					}
					result = sb.toString();
					Log.d("result", result);
				} catch (Exception e) {
				} finally {
					try {
						if (inputStream != null)
							inputStream.close();
					} catch (Exception squish) {
					}
				}

				JSONObject jObject;
				try {
					jObject = new JSONObject(result.toString());
					gameID = jObject.getInt("gameID");

					Log.d("id", gameID + "");
				} catch (JSONException e1) {
					e1.printStackTrace();
				}

			}
			return String.valueOf(gameID);
		}

	}

	private class MyUpdatePlayersTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... args) {

			// Add the new components for the row to the TableLayout

			String sendURL = null;
			if (gameID >= 0) {
				sendURL = getScoresURL + gameID;
			} else {
				sendURL = args[1];
			}

			Log.d("url", sendURL);
			DefaultHttpClient httpclient = new DefaultHttpClient(
					new BasicHttpParams());

			HttpPost httppost = new HttpPost(sendURL);
			httppost.setHeader("Content-type", "application/json");

			InputStream inputStream = null;

			String result = null;

			try {
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				inputStream = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inputStream, "UTF-8"), 8);
				StringBuilder sb = new StringBuilder();

				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				result = sb.toString();
				Log.d("result", result);
			} catch (Exception e) {
			} finally {
				try {
					if (inputStream != null)
						inputStream.close();
				} catch (Exception squish) {
				}
			}

			JSONArray jArray = null;
			try {
				Log.d("jArray", result.toString());

				jArray = new JSONArray(result);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}

			playersAndScores.clear();

			if (jArray != null) {
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject jObject;
					String user = null;
					int score = 0;
					try {
						jObject = jArray.getJSONObject(i);
						user = jObject.getString("username");
						score = jObject.getInt("score");

						playersAndScores.add(user);
						playersAndScores.add(Integer.toString(score));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				// Add the new components for the row to the TableLayout

			}
			return null;
		}

		protected void onPostExecute(String result) {

			for (int i = 0; i < playersAndScores.size() - 1; i += 2) {
				// Get the LayoutInflater service
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				// Use the inflater to inflate a join row from
				View newGamePlayerRow = inflater.inflate(
						R.layout.game_player_row, null);

				TextView playerName = (TextView) newGamePlayerRow
						.findViewById(R.id.gamePlayerName);

				TextView playerScore = (TextView) newGamePlayerRow
						.findViewById(R.id.gamePlayerScore);
				playerName.setText(playersAndScores.get(i));
				playerScore.setText(playersAndScores.get(i + 1));
				Log.d("newGamePlayerRow", newGamePlayerRow.toString());
				gamePlayersScrollView.addView(newGamePlayerRow);
			}

		}

	}

	private class MyAsyncTask extends AsyncTask<String, String, String> {

		protected String doInBackground(String... args) {

			String sendURL = args[0];

			DefaultHttpClient httpclient = new DefaultHttpClient(
					new BasicHttpParams());

			HttpPost httppost = new HttpPost(sendURL);
			httppost.setHeader("Content-type", "application/json");

			InputStream inputStream = null;

			String result = null;

			try {
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				inputStream = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inputStream, "UTF-8"), 8);
				StringBuilder sb = new StringBuilder();

				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				result = sb.toString();
				Log.d("result", result);
			} catch (Exception e) {

			} finally {
				try {
					if (inputStream != null)
						inputStream.close();
				} catch (Exception squish) {
				}
			}

			JSONArray jArray = null;
			try {
				Log.d("jArray", result.toString());

				jArray = new JSONArray(result);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}

			if (jArray != null) {
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject jObject;
					String photoURL = null;
					double photoLat = 0;
					double photoLon = 0;
					try {
						jObject = jArray.getJSONObject(i);
						photoURL = jObject.getString("url");
						photoLat = jObject.getDouble("lat");
						photoLon = jObject.getDouble("lon");

					} catch (JSONException e) {
						e.printStackTrace();
					}

					URL url;
					InputStream content = null;
					try {
						url = new URL(photoURL);
						content = (InputStream) url.getContent();
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					Drawable d = Drawable.createFromStream(content, "src");

					drawRet.add(d);
					ret.add(photoLat);
					ret.add(photoLon);

				}
			}

			return null;
		}

		protected void onPostExecute(String result) {

			for (int i = 0; i < ret.size() - 1; i += 2) {
				Drawable photo = drawRet.get(i / 2);
				Bitmap c = ((BitmapDrawable) photo).getBitmap();
				Bitmap b = c.createScaledBitmap(c, IMAGE_WIDTH, IMAGE_HEIGHT,
						true);

				double lat = ret.get(i);
				double lon = ret.get(i + 1);

				LatLng photoPosition = new LatLng(lat, lon);

				Marker currentMark = map
						.addMarker(new MarkerOptions()
								.position(photoPosition)
								.icon(BitmapDescriptorFactory
										.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
				unfoundMapMarkers.add(currentMark);
				images.put(currentMark, b);
				// .icon(BitmapDescriptorFactory.fromBitmap(cS)));

			}

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

	protected void onStart() {
		super.onStart();
		if (this.servicesConnected()) {
			mLocationClient.connect();
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
			int errorCode = resultCode; // connectionResult.getErrorCode();
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
