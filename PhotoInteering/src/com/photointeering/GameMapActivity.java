package com.photointeering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

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
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

public class GameMapActivity extends Activity {

	private static final String TAG = "PHOTO";

	ImageView image;
	TextView latitudeTextView;
	TextView longitudeTextView;
	ArrayList<Double> ret = new ArrayList<Double>();
	ArrayList<Drawable> drawRet = new ArrayList<Drawable>();

	static final String KEY_PHOTO_URL = "url";
	static final String KEY_PHOTO_LAT = "lat";
	static final String KEY_PHOTO_LON = "lon";

	String photoURL = "";
	double latitude = 0.0;
	double longitude = 0.0;

	String newGameURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/new_game/";
	String joinGameURL = "http://plato.cs.virginia.edu/~cs4720s14asparagus/join_game/";

	private TableLayout photoScrollView;

	GoogleMap map;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		// setContentView(R.layout.activity_game_map);

		photoScrollView = (TableLayout) findViewById(R.id.photoScrollViewTable);

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
			sendURL = newGameURL + currentLat + "/" + currentLon;
		}
		else {
			sendURL = joinGameURL + gameID + "/" + "test";
		}

		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();
		LatLng current = new LatLng(currentLatDouble, currentLonDouble);
		Marker currentMark = map.addMarker(new MarkerOptions()
				.position(current).title("Start"));
		// .snippet("Kiel is cool"));
		// .icon(BitmapDescriptorFactory
		// .fromResource(R.drawable.ic_launcher)));

		map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 0));

		// Zoom in, animating the camera.
		map.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

		// TextView newImageLat = (TextView)
		// findViewById(R.id.currentLatTextView);
		// newImageLat.setText("Latitude " + currentLat);
		//
		// TextView newImageLon = (TextView)
		// findViewById(R.id.currentLonTextView);
		// newImageLon.setText("Longitude " + currentLon);

		new MyAsyncTask().execute(sendURL);
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
				double lat = ret.get(i);
				double lon = ret.get(i + 1);

				LatLng photoPosition = new LatLng(lat, lon);
				
				Marker currentMark = map
						.addMarker(new MarkerOptions()
								.position(photoPosition)
								.icon(BitmapDescriptorFactory
										.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

			}

			// // Get the LayoutInflator service
			// LayoutInflater inflater = (LayoutInflater)
			// getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			//
			// // Use the inflater to inflate a join row from
			// // game_map_row.xml
			// View newPhotoRow = inflater
			// .inflate(R.layout.game_map_row, null);
			//
			// ImageView newImageView = (ImageView) newPhotoRow
			// .findViewById(R.id.image);
			//
			// newImageView.setImageDrawable(photo);
			//
			//
			// TextView newImageLat = (TextView) newPhotoRow
			// .findViewById(R.id.latitudeTextView);
			// String string_lat = Double.toString(lat);
			// newImageLat.setText(string_lat);
			//
			// TextView newImageLon = (TextView) newPhotoRow
			// .findViewById(R.id.longitudeTextView);
			// String string_long = Double.toString(lon);
			// newImageLon.setText(string_long);
			//
			// // Add the new components for the stock to the TableLayout
			// Log.d("newPhotoRow", newPhotoRow.toString());
			// photoScrollView.addView(newPhotoRow);

			// }

		}

	}
}
