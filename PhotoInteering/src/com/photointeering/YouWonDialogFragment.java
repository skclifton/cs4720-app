package com.photointeering;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.photointeering.R;

public class YouWonDialogFragment extends DialogFragment {
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.you_won)
				.setMessage(
						"Hit the back button to start a new game or join an existing game near you.")
				.setPositiveButton(R.string.continue_button,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						}).setCancelable(false);
		// Create the AlertDialog object and return it
		return builder.create();
	}
}
