package com.coolhandsoftware.trailtrace;

import com.coolhandsoftware.topogen.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class NoNetworkDialogFragment extends DialogFragment {
	
	/**
	 * Callback interface for the no network dialog. Must be implemented by NoNetworkDialogFragment's owner.
	 */
	public interface INoNetworkDialogListener {
		public void onDialogPosClick(DialogFragment dialog);
	}
	
	INoNetworkDialogListener mListener;
	
	/**
	 * Instantiates mListener, or throws an exception if it can't.
	 */
	@Override
	public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the owner thru it
            mListener = (INoNetworkDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement INoNetworkDialogListener");
        }

	}
	
	/**
	 * Builds callback listeners to the mListener.
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    builder.setMessage(R.string.nonetworkdialog_no_network_what_do)
	           .setPositiveButton(R.string.nonetworkdialog_ok, new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                   mListener.onDialogPosClick(NoNetworkDialogFragment.this);
	               }
	           });
	    return builder.create();
	}
}
