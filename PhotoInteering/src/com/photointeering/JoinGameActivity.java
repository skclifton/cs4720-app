package com.photointeering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

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
	
	//map to keep count of each gameID, as well as to which game players belong
	HashMap<Integer,Integer> IDMap;
	HashMap<String,Integer> playerMap;

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
			
			IDMap = new HashMap<Integer, Integer>();
			playerMap = new HashMap<String, Integer>();
			
			if (jArray != null) {
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject jObject;
					
					//get hash mapping ids to counts
					// for each new json object get id, 
					// if id is in map, then increment count
					// if not, then add it to map and set count to 1.
					
					String gameCreator = null;
					int gameID = 0;
										
					try {
						jObject = jArray.getJSONObject(i);						
						gameCreator = jObject.getString("username");
						gameID = jObject.getInt("gameID");

					} catch (JSONException e) {
						Log.e("error", e.getMessage());					
					}

					playerMap.put(gameCreator, gameID);
					
					if (! IDMap.containsKey(gameID)) {
						IDMap.put(gameID, 1);
					} else {						
						int increment = IDMap.get(gameID) + 1;
						IDMap.put(gameID, increment);
					}
					
					String gameID_str = gameID + "";									
//					
					ret.add(gameID_str);
					ret.add(gameCreator);
				}
			}

			return null;
		}

		protected void onPostExecute(String result) {
					
			Log.v("IDMap", IDMap.toString());
			
			//initialize arrays to hold keys from IDMap and the playerMap
			Object[] gameIDs;
			gameIDs = IDMap.keySet().toArray();

			Object[] players;
			players = playerMap.keySet().toArray();
			
			//create map from IDs to players, this is the player that will show up in full in the menu
			HashMap<Integer, String> mainPlayers = new HashMap<Integer, String>();
			
			
			// go through all of the 
			for (int i = 0; i < gameIDs.length ; i += 1) {
				
				
				
				//establish which player name is listed on the join row.
				//It is the first player that is associated with the current game id.
				for (int j = 0; j < players.length; j+=1){
					if(playerMap.get(players[j]) == gameIDs[i]) {
						if (! mainPlayers.containsKey(gameIDs[i])) {
							mainPlayers.put((Integer) gameIDs[i], (String) players[j]);	
						} 						
						break;
					}						
					
				}
				
				// Get the LayoutInflator service
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				// Use the inflater to inflate a join row from
				// join_game_row.xml
				View newJoinRow = inflater
						.inflate(R.layout.join_game_row, null);

				TextView GameID = (TextView) newJoinRow
						.findViewById(R.id.GameIDTextView);
				
				String id_str = gameIDs[i].toString();
				GameID.setText(id_str);

				TextView gameCreator = (TextView) newJoinRow
						.findViewById(R.id.GameCreatorTextView);
				
				String player_string = mainPlayers.get(gameIDs[i]) + " + " + (IDMap.get(gameIDs[i]) - 1) + " others playing"; 
				gameCreator.setText(player_string);

				// Add the new components for the row to the TableLayout
				Log.d("newJoinRow", newJoinRow.toString());
				joinScrollView.addView(newJoinRow);

			}

		}

	}

}
