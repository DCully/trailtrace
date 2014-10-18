package com.coolhandsoftware.trailtrace;

import java.util.List;

import com.coolhandsoftware.topogen.R;

import android.content.Context;
import android.location.Address;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * This class overrides ArrayAdapter<Address> to provide a better presentation/layout of Address information
 * for each View in the ListView by overriding the getView method.
 * @author David Cully
 * david.a.cully@gmail.com
 *
 */
public class AddressListAdapter extends ArrayAdapter<Address>
{
	private final Context context;
	private final List<Address> addresses;
	
	static class ViewHolder {
		public TextView line1TextView;
		public TextView line2TextView;
	}
	
	public AddressListAdapter(Context context, List<Address> objects) {
		super(context, R.layout.row_view, objects);
		this.context = context;
		this.addresses = objects;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		View rowView = convertView;
		if (rowView == null) { // then it's never been inflated before
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.row_view, parent, false);		
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.line1TextView = (TextView) rowView.findViewById(R.id.line_1);
			viewHolder.line2TextView = (TextView) rowView.findViewById(R.id.line_2);
			rowView.setTag(viewHolder); // this makes future lookups faster by saving us a findViewById call later on
		}

		// since the above code guarantees that every rowView has a ViewHolder, we can rely on it now
		ViewHolder theHolder = (ViewHolder) rowView.getTag();
		
		// these are the strings we want to display in the rowView
//		String line1Text = addresses.get(position).getFeatureName();
//		String line2Text = addresses.get(position).getLocality() + " - " + addresses.get(position).getCountryName();
		
		String formattedAddressName = addresses.get(position).getExtras().getString("display_name");
		if (formattedAddressName == null) {
			formattedAddressName = "I'm Feeling Lucky!";
		}
		
		String latitudeAndLongitude = "Latitude: " + addresses.get(position).getLatitude() 
				+ "    Longitude: " + addresses.get(position).getLongitude();
		
		// these two lines actually set textViews to say the correct thing (thru the ViewHolder)
		theHolder.line1TextView.setText(formattedAddressName);
		theHolder.line2TextView.setText(latitudeAndLongitude);
		
		return rowView;
	}
	
}