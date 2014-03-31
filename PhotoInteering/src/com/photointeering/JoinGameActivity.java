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
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;


public class JoinGameActivity extends Activity {
	
	private static final String TAG = "PHOTO";

	ImageView image;
	
	TextView GameCreatorTextView;
	TextView GameIDTextView;
	
	
	// ret changed to carry strings.
	ArrayList<String> ret = new ArrayList<String>();
	ArrayList<Drawable> drawRet = new ArrayList<Drawable>();

	static final String KEY_PHOTO_URL = "url";
	static final String KEY_PHOTO_LAT = "lat";
	static final String KEY_PHOTO_LON = "lon";

	String photoURL = "";
	double latitude = 0.0;
	double longitude = 0.0;

	String url = "http://asparagus-phase3.azurewebsites.net/";

	private TableLayout joinScrollView;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_join_game);

		joinScrollView = (TableLayout) findViewById(R.id.joinScrollViewTable);

		Intent intent = getIntent();		
		GameIDTextView = (TextView) findViewById(R.id.GameIDTextView);
		GameCreatorTextView = (TextView) findViewById(R.id.GameCreatorTextView);

		String currentLat = Double.toString(intent.getDoubleExtra("lat", 0.0));
		String currentLon = Double.toString(intent.getDoubleExtra("lon", 0.0));
		String gpsCoords = intent.getStringExtra(MainActivity.GPS_COORDS);

		final String sendURL = url + currentLat + "/" + currentLon;

//		TextView newImageLat = (TextView) findViewById(R.id.currentLatTextView);
//		newImageLat.setText("Latitude " + currentLat);
//
//		TextView newImageLon = (TextView) findViewById(R.id.currentLonTextView);
//		newImageLon.setText("Longitude " + currentLon);

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
					
					String gameCreator = null;
					int gameID = 0;
					try {
						jObject = jArray.getJSONObject(i);
						photoURL = jObject.getString("url");
						gameCreator = jObject.getString("username");
						gameID = jObject.getInt("gameID");

					} catch (JSONException e) {
						e.printStackTrace();
					}

					
					// Photo data not needed for join game activity.
//					URL url;
//					InputStream content = null;
//					try {
//						url = new URL(photoURL);
//						content = (InputStream) url.getContent();
//					} catch (MalformedURLException e) {
//						e.printStackTrace();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//					Drawable d = Drawable.createFromStream(content, "src");
					
					String gameID_str = gameID + "";									
//					
//					drawRet.add(d);
					ret.add(gameID_str);
					ret.add(gameCreator);

				}
			}

			return null;
		}

		protected void onPostExecute(String result) {
					
			Log.v("IBIBBI", "The size of ret is " + ret.size());

			for (int i = 0; i < ret.size() - 1; i += 2) {
//				Drawable photo = drawRet.get(i / 2);
				String id = ret.get(i);
				String creator = ret.get(i + 1);

				// Get the LayoutInflator service
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				// Use the inflater to inflate a join row from
				// join_game_row.xml
				View newJoinRow = inflater
						.inflate(R.layout.join_game_row, null);

//				ImageView newImageView = (ImageView) newPhotoRow
//						.findViewById(R.id.image);
//
//				newImageView.setImageDrawable(photo);
				

				TextView GameID = (TextView) newJoinRow
						.findViewById(R.id.GameIDTextView);
//				String string_lat = Double.toString(lat);
				GameID.setText(id);

				TextView gameCreator = (TextView) newJoinRow
						.findViewById(R.id.GameCreatorTextView);
//				String string_long = Double.toString(lon);
				gameCreator.setText(creator);

				// Add the new components for the stock to the TableLayout
				Log.d("newPhotoRow", newJoinRow.toString());
				joinScrollView.addView(newJoinRow);

			}

		}

	}

}
