package com.pjaol.search;

public class Constants {
	
	public final static String KML_HEADER= 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+"<kml xmlns=\"http://www.opengis.net/kml/2.2\">";
	
	public final static String KML_PLACE_MARKER_HEADER=	"<Placemark>\n";
	public final static String KML_PLACE_MARKER_FOOTER=	"</Placemark>\n";
	public final static String KML_FOOT = "</kml>";
	
	public final static String KML_POLYGON_HEADER = "<Polygon>\n<outerBoundaryIs>\n";
	public final static String KML_POLYGON_FOOTER = "</outerBoundaryIs>\n</Polygon>";
	
	public final static String KML_DOCUMENT_HEADER= "<Document>\n"
										+"<Style id=\"randomColorIcon\">\n"
										+"<IconStyle>\n"
										+"<color>ff00ff00</color>\n"
										+"<colorMode>random</colorMode>\n"
										+"<scale>1.1</scale>\n"
										+"<Icon>\n"
										+"<href>http://maps.google.com/mapfiles/kml/pal3/icon21.png</href>\n"
										+"</Icon>\n"
										+"</IconStyle>"
										+"</Style>";		
	public final static String KML_DOCUMENT_FOOTER = "</Document>";
	
	
	public final static String KML_POINT_HEADER = "<Point>";
	public final static String KML_POINT_FOOTER = "</Point>";
	
	public final static String KML_COORDINATES_HEADER = "<coordinates>";
	public final static String KML_COORDINATES_FOOTER = "</coordinates>";

}
