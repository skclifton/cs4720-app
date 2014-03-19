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
	ArrayList<String> ret = new ArrayList<String>();
	ArrayList<Drawable> drawRet = new ArrayList<Drawable>();

	static final String KEY_PHOTO_URL = "url";
	static final String KEY_PHOTO_LAT = "lat";
	static final String KEY_PHOTO_LON = "lon";

	String photoURL = "";
	double latitude = 0.0;
	double longitude = 0.0;

	String url = "http://plato.cs.virginia.edu/~cs4720s14asparagus/new_game/";

	private TableLayout photoScrollView;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game_map);

		photoScrollView = (TableLayout) findViewById(R.id.photoScrollViewTable);

		Intent intent = getIntent();
		image = (ImageView) findViewById(R.id.image);
		latitudeTextView = (TextView) findViewById(R.id.latitudeTextView);
		longitudeTextView = (TextView) findViewById(R.id.longitudeTextView);

		String currentLat = Double.toString(intent.getDoubleExtra("lat", 0.0));
		String currentLon = Double.toString(intent.getDoubleExtra("lon", 0.0));
		String gpsCoords = intent.getStringExtra(MainActivity.GPS_COORDS);

		final String sendURL = url + currentLat + "/" + currentLon;

		TextView newImageLat = (TextView) findViewById(R.id.currentLatTextView);
		newImageLat.setText("Latitude " + currentLat);

		TextView newImageLon = (TextView) findViewById(R.id.currentLonTextView);
		newImageLon.setText("Longitude " + currentLon);

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
				jArray = new JSONArray(result);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (jArray != null) {
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject jObject;
					String photoURL = null;
					String photoLat = "0.0";
					String photoLon = "0.0";
					try {
						jObject = jArray.getJSONObject(i);
						photoURL = jObject.getString("url");
						photoLat = jObject.getString("lat");
						photoLon = jObject.getString("lon");

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
				String lat = ret.get(i);
				String lon = ret.get(i + 1);

				// Get the LayoutInflator service
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				// Use the inflater to inflate a stock row from
				// stock_quote_row.xml
				View newPhotoRow = inflater
						.inflate(R.layout.game_map_row, null);

				ImageView newImageView = (ImageView) newPhotoRow
						.findViewById(R.id.image);

				newImageView.setImageDrawable(photo);
				

				TextView newImageLat = (TextView) newPhotoRow
						.findViewById(R.id.latitudeTextView);
				newImageLat.setText(lat);

				TextView newImageLon = (TextView) newPhotoRow
						.findViewById(R.id.longitudeTextView);
				newImageLon.setText(lon);

				// Add the new components for the stock to the TableLayout
				Log.d("newPhotoRow", newPhotoRow.toString());
				photoScrollView.addView(newPhotoRow);

			}

		}

	}
}
