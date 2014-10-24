package com.coolhandsoftware.trailtrace;

import android.app.DialogFragment;

/**
 * Callback interface for the no network dialog. Must be implemented by NoNetworkDialogFragment's owner.
 * @author David Cully
 * david.a.cully@gmail.com
 *
 */
public interface INoNetworkDialogListener {
	public void onDialogPosClick(DialogFragment dialog);
}
