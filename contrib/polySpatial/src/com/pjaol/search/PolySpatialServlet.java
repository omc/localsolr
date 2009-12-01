package com.pjaol.search;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.spatial.tier.projections.XYProjector;
import org.apache.lucene.util.NumericUtils;

import com.pjaol.search.PointsD;

public class PolySpatialServlet extends HttpServlet {

	polySpatialTest pst = new polySpatialTest();
	int level = 16;
	// private IProjector projector = new SinusoidalProjector();
	private IProjector projector = new XYProjector();
	CartesianTierPlotter ctp;
	Pattern brackets = Pattern.compile("[^\\(](.*?)\\)"); // ugh would like to
															// add [^\\)] but no
															// such luck

	private String ACTION = "action";
	private String POINTS = "points";

	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);
		try {
			pst.setUp();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ctp = new CartesianTierPlotter(level, projector,
				CartesianTierPlotter.DEFALT_FIELD_PREFIX);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String action = req.getParameter(ACTION);
		String ps = req.getParameter(POINTS);
		List<PointsD> points = parsePoints(ps);
		List<TierBoundaries> boundaries = new ArrayList<TierBoundaries>();
		for (PointsD p : points) {
			boundaries.add(getBoundaries(p));
		}

		PrintWriter writer = resp.getWriter();
		writer.write(Constants.KML_HEADER);
		writer.write(Constants.KML_PLACE_MARKER_HEADER);
		writer.write("<name>LinearRing.kml</name>");
		int c = 0;
		for (TierBoundaries tb : boundaries) {
			writer.write(Constants.KML_POLYGON_HEADER);
			writer.write("<LinearRing id=\"poly_" + c + "\">");
			writer.write("<coordinates>\n");
			writer.write(tb.toKMLCoords());
			writer.write("</coordinates>\n");
			writer.write("</LinearRing>");
			writer.write(Constants.KML_POLYGON_FOOTER);
			c++;
		}
		writer.write(Constants.KML_PLACE_MARKER_FOOTER);

		CartesianConvexHull cch = new CartesianConvexHull(ctp
				.getTierFieldName());
		cch.setPlotter(ctp);

		List<Double> boxes = pst.pointsToBoxIds(ctp, points);
		for (Double bid : boxes) {
			System.out.println("===>"+bid);
			cch.addBox(bid);
		}

		cch.closeLoop();
		TopDocs td = null;
		try {
			td = pst.performShapeSearch(ctp, cch);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Found : "+td.totalHits);
		
		if (td.totalHits > 0) {
			writer.write(Constants.KML_DOCUMENT_HEADER);
			ScoreDoc[] sds = td.scoreDocs;
			IndexSearcher is = pst.getIndexSearcher();
			for (ScoreDoc sd : sds) {
				Document d = is.doc(sd.doc);
				writer.write(Constants.KML_PLACE_MARKER_HEADER);
				String id = d.getField("name").stringValue();
				double lat = NumericUtils.prefixCodedToDouble(d.getField("lat")
						.stringValue());
				double lng = NumericUtils.prefixCodedToDouble(d.getField("lng")
						.stringValue());

				writer.write("<name>" + id + "</name>\n");
				writer.write("<styleUrl>#randomColorIcon</styleUrl>");
				writer.write(Constants.KML_POINT_HEADER);
				
				writer.write(Constants.KML_COORDINATES_HEADER);
				writer.write(lng + "," + lat);
				writer.write(Constants.KML_COORDINATES_FOOTER);
				
				writer.write(Constants.KML_POINT_FOOTER);
				writer.write(Constants.KML_PLACE_MARKER_FOOTER);
			}
			
			writer.write(Constants.KML_DOCUMENT_FOOTER);
		}
		/*
		 * <Document> <Style id="randomColorIcon"> <IconStyle>
		 * <color>ff00ff00</color> <colorMode>random</colorMode>
		 * <scale>1.1</scale> <Icon>
		 * <href>http://maps.google.com/mapfiles/kml/pal3/icon21.png</href>
		 * </Icon> </IconStyle> </Style> <Placemark> <name>IconStyle.kml</name>
		 * <styleUrl>#randomColorIcon</styleUrl> <Point>
		 * <coordinates>-122.36868,37.831145,0</coordinates> </Point>
		 * </Placemark> </Document>
		 */

		writer.write(Constants.KML_FOOT);
		writer.close();

	}

	public static void main(String[] args) {
		PolySpatialServlet pss = new PolySpatialServlet();
		List<PointsD> pd = pss
				.parsePoints("(-37.43824119865275,-122.13775634764588)(37.43824119865275,-122.13876773332271)(37.43612168200744,-122.13775634764588)(37.43612168200744,-122.13876773332271)");
		for (PointsD p : pd) {
			System.out.println(p);
		}

		List<Double> boxIds = pss.pst.pointsToBoxIds(pss.ctp, pd);
	}

	private List<PointsD> parsePoints(String points) {
		List<PointsD> ps = new ArrayList<PointsD>();
		Map<Double, Double> done = new HashMap<Double, Double>();

		Matcher m = brackets.matcher(points);
		while (m.find()) {
			String cp = m.group();
			cp = cp.replaceAll("\\)", "");
			String[] ll = cp.split(",");

			double lat, lng;
			lat = new Double(ll[0]);
			lng = new Double(ll[1]);
			PointsD np = new PointsD(lat, lng);

			if (!ps.contains(np))
				ps.add(new PointsD(lat, lng));
		}

		return ps;
	}

	public TierBoundaries getBoundaries(PointsD p) {
		TierBoundaries tb = new TierBoundaries();
		tb.getBoundaries(p.getX(), p.getY());
		return tb;
	}

}
