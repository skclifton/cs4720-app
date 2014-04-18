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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

public class GameMapActivity extends Activity implements OnMarkerClickListener {

	int IMAGE_WIDTH = 256;
	int IMAGE_HEIGHT = 180;

	private static final String TAG = "PHOTO";

	ImageView image;
	TextView latitudeTextView;
	TextView longitudeTextView;
	ArrayList<Double> ret = new ArrayList<Double>();
	ArrayList<Drawable> drawRet = new ArrayList<Drawable>();
	ArrayList<String> playersAndScores = new ArrayList<String>();

	static final String KEY_PHOTO_URL = "url";
	static final String KEY_PHOTO_LAT = "lat";
	static final String KEY_PHOTO_LON = "lon";

	String photoURL = "";
	double latitude = 0.0;
	double longitude = 0.0;

	String newGameURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/new_game/";
	String joinGameURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/join_game/";
	String getScoresURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/get_scores/";
	String getNewGameURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/get_recent_game/";

	private TableLayout gamePlayersScrollView;

	// private TableLayout photoScrollView;

	GoogleMap map;
	private View infoWindow;
	public HashMap images = new HashMap<Marker, Bitmap>();

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		// setContentView(R.layout.activity_game_map);

		// photoScrollView = (TableLayout)
		// findViewById(R.id.photoScrollViewTable);
		gamePlayersScrollView = (TableLayout) findViewById(R.id.gamePlayersTableLayout);

		Intent intent = getIntent();
		image = (ImageView) findViewById(R.id.image);
		latitudeTextView = (TextView) findViewById(R.id.latitudeTextView);
		longitudeTextView = (TextView) findViewById(R.id.longitudeTextView);

		Double currentLatDouble = intent.getDoubleExtra("lat", 0.0);
		Double currentLonDouble = intent.getDoubleExtra("lon", 0.0);

		String currentLat = Double.toString(currentLatDouble);
		String currentLon = Double.toString(currentLonDouble);
		String gpsCoords = intent.getStringExtra(MainActivity.GPS_COORDS);
		boolean newGame = intent.getBooleanExtra("newGame", true);
		int gameID = intent.getIntExtra("gameID", 0);
		String sendURL = "";

		if (newGame) {
			sendURL = newGameURL + currentLat + "/" + currentLon + "/"
					+ getAccountName();
		} else {
			sendURL = joinGameURL + gameID + "/" + "test";
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

		TextView player = (TextView) findViewById(R.id.playerTextView);
		player.setText(getAccountName());

		TextView photosFound = (TextView) findViewById(R.id.photosFoundTextView);
		photosFound.setText("0");

		updatePlayers(gameID, newGame);

		new MyAsyncTask().execute(sendURL);
	}

	public void updatePlayers(int gameID, boolean isNew) {

		String sendURL = "";
		String newGameURL = "";
		if (isNew) {
			newGameURL = getNewGameURL + "test";
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

	private class MyUpdatePlayersTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... args) {

			String SendURL1 = args[0];
			int id = -1;
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
				//
				// JSONArray jArray = null;
				// try {
				// Log.d("jArray", result.toString());
				//
				// jArray = new JSONArray(result);
				// } catch (JSONException e1) {
				// // TODO Auto-generated catch block
				// e1.printStackTrace();
				// }

				JSONObject jObject;
				try {
					jObject = new JSONObject(result.toString());
					id = jObject.getInt("gameID");
					Log.d("id", id + "");
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				// Add the new components for the row to the TableLayout

			}

			String sendURL = null;
			if (id >= 0) {
				sendURL = getScoresURL + id;
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
				// TODO Auto-generated catch block
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
				// Get the LayoutInflator service
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
				// TODO Auto-generated catch block
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

				images.put(currentMark, b);
				// .icon(BitmapDescriptorFactory.fromBitmap(cS)));

			}

		}

	}

}
