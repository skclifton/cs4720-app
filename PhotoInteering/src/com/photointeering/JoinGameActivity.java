package com.photointeering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.photointeering.R.color;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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

	// map to keep count of each gameID, as well as to which game players belong
	HashMap<Integer, Tuple> IDMap;

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

			IDMap = new HashMap<Integer, Tuple>();

			if (jArray != null) {
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject jObject;

					String gameCreator = null;
					int gameID = 0;

					try {
						jObject = jArray.getJSONObject(i);
						gameCreator = jObject.getString("starter");
						gameID = jObject.getInt("gameID");

					} catch (JSONException e) {
						Log.e("error", e.getMessage());
					}

					if (!IDMap.containsKey(gameID)) {
						Tuple t = new Tuple(gameCreator, 1);
						IDMap.put(gameID, t);
					} else {
						Tuple t = IDMap.get(gameID);
						t.increment_players();
						IDMap.put(gameID, t);
					}

				}

			}

			return null;
		}

		protected void onPostExecute(String result) {

			Log.v("IDMap", IDMap.toString());

			// create map from IDs to players, this is the player that will show
			// up in full in the menu
			// HashMap<Integer, String> mainPlayers = new HashMap<Integer,
			// String>();

			for (Map.Entry<Integer, Tuple> entry : IDMap.entrySet()) {
				System.out.println(entry.getKey() + " " + entry.getValue());

				// Get the LayoutInflator service
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

				// Use the inflater to inflate a join row from
				// join_game_row.xml
				View newJoinRow = inflater
						.inflate(R.layout.join_game_row, null);

				TextView GameID = (TextView) newJoinRow
						.findViewById(R.id.GameIDTextView);

				String id_str = entry.getKey().toString();

				GameID.setText(id_str);

				TextView gameCreator = (TextView) newJoinRow
						.findViewById(R.id.GameCreatorTextView);

				String gameCreatorUsername = entry.getValue().getGameOwner();
				// int num_in_game = entry.getValue().getNumPlayers();
				//
				// String player_string = displayedPlayer + " + "
				// + (num_in_game - 1) + " others playing";
				//
				gameCreator.setText(gameCreatorUsername);

				Button joinRowButton = (Button) newJoinRow
						.findViewById(R.id.joinGameRowButton);
				joinRowButton.setOnClickListener(new JoinClickListener(Integer
						.parseInt(id_str)));

				// Add the new components for the row to the TableLayout
				Log.d("newJoinRow", newJoinRow.toString());

				// Can be used to add different colored lines later
				// if (i%2 == 1) {
				// newJoinRow.setBackgroundColor(color.dark_cream);
				// }
				// else {
				// newJoinRow.setBackgroundColor(color.cream);
				// }

				joinScrollView.addView(newJoinRow);

			}

		}

	}

	public class JoinClickListener implements OnClickListener {
		int id;

		public JoinClickListener(int id) {
			this.id = id;
		}

		public void onClick(View v) {

			Intent intent = getIntent();

			Double currentLatDouble = intent.getDoubleExtra("lat", 0.0);
			Double currentLonDouble = intent.getDoubleExtra("lon", 0.0);
			boolean newGame = intent.getBooleanExtra("newGame", false);

			int gameID = this.id;

			Intent mapGame = new Intent(JoinGameActivity.this,
					GameMapActivity.class);
			mapGame.putExtra("lat", currentLatDouble);
			mapGame.putExtra("lon", currentLonDouble);
			mapGame.putExtra("newGame", newGame);
			mapGame.putExtra("gameID", gameID);

			startActivity(mapGame);
		}
	}

}
