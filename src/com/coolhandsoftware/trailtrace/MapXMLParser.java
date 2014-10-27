package com.coolhandsoftware.trailtrace;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;


/**
 * Class to parse the map data from the XML file's InputStream.
 * 
 * @author David Cully (david.a.cully@gmail.com)
 *
 */
public class MapXMLParser {

	/** input stream produced by HttpUrlConnection, passed in thru ctor **/
	private InputStream mStream;
	private XmlPullParser mParser;
	
	/**
	 * Build an instance in order to use the given stream to produce returns for its methods.
	 * @param inStream The stream from the XML file
	 */
	public MapXMLParser(InputStream inStream) throws XmlPullParserException, IOException {
		mStream = inStream;
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser mParser = factory.newPullParser();
        mParser.setInput(inStream, null);
	}
	
	public void spitOutOpenTags() throws IOException, XmlPullParserException {
		int eventType = mParser.getEventType();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if(eventType == XmlPullParser.START_TAG) {
				System.out.println("Start tag "+mParser.getName());
				eventType = mParser.next();
			}
		}
	}
	
	/**
	 * Get the next OSM "way", as defined by OSM's format.
	 * @return The next Way object, or null if we've read them all.
	 */
	public Way readNextWay() {
		
		
		return null;
	}

}
