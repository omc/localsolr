package com.pjaol.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.spatial.geohash.GeoHashUtils;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.spatial.tier.projections.XYProjector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;

public class polySpatialTest {

	private Directory directory;
	private IndexSearcher searcher;
	private String latField = "lat";
	private String lngField = "lng";
	private List<CartesianTierPlotter> ctps = new LinkedList<CartesianTierPlotter>();
	private String geoHashPrefix = "_geoHash_";
	//private IProjector project = new SinusoidalProjector();
	private IProjector project = new XYProjector();
	private IndexSearcher is;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		polySpatialTest pst = new polySpatialTest();
		try {
			pst.setUp();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			pst.setUpShape();
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setUpShape() throws CorruptIndexException, IOException{
		List<PointsD> points = new ArrayList<PointsD>();
		points.add(new PointsD(37.43629392840975,-122.13848663330079));
		points.add(new PointsD(37.446293928409744,-122.12848663330078));
		points.add(new PointsD(37.43629392840975,-122.11848663330078));
		points.add(new PointsD(37.42629392840975,-122.12848663330078));
		points.add(new PointsD(37.43629392840975,-122.13848663330079));
		
		int level = 16;
		CartesianTierPlotter ctp = new CartesianTierPlotter(level, project, CartesianTierPlotter.DEFALT_FIELD_PREFIX);
		
		System.out.println("Converting points to boxes");
		
		List<Double>boxs = pointsToBoxIds(ctp, points);
		CartesianConvexHull cch = new CartesianConvexHull(ctp.getTierFieldName());
		
		cch.setPlotter(ctp);
		for(Double bid: boxs){
			System.out.println(bid);
			cch.addBox(bid);
		}
		cch.closeLoop();
		System.out.println("================");
		
		TopDocs td = null;
		try {
			performShapeSearch(ctp, cch);
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Found: "+td.totalHits);
		ScoreDoc[] sds = td.scoreDocs;
		
		for(ScoreDoc sd: sds){
			
			Document d = is.doc(sd.doc);
			System.out.println(d.getField("name").stringValue()+" "
							   +NumericUtils.prefixCodedToDouble(d.getField("lat").stringValue())
							   +" "+NumericUtils.prefixCodedToDouble(d.getField("lng").stringValue())
							   +" "+NumericUtils.prefixCodedToDouble(d.getField(ctp.getTierFieldName()).stringValue()));
		}
	}
	
	
	public TopDocs performShapeSearch(CartesianTierPlotter ctp, CartesianConvexHull cch) throws CorruptIndexException, IOException, ParseException{
		
		IndexSearcher is = new IndexSearcher(directory);

		List<RangeItems>ranges = cch.getFillAsRanges();
		BooleanQuery bq = new BooleanQuery();
		for(RangeItems r : ranges){
			bq.add(NumericRangeQuery.newDoubleRange(ctp.getTierFieldName(), r.getLower(), r.getUpper(), true, true),
					Occur.SHOULD);
		}
		Query q = NumericRangeQuery.newDoubleRange(ctp.getTierFieldName(), -35312.44465, -35312.44465, true, true);
		
		
		System.out.println(bq);
		TopDocs td = is.search(bq, 100);
		
		
		
		return td;
	}
	
	
	public List<Double> pointsToBoxIds(CartesianTierPlotter ctp, List<PointsD> points){
		
		List<Double> boxs = new ArrayList<Double>();
		for(PointsD p: points){
			boxs.add(ctp.getTierBoxId(p.getX(), p.getY()));
		}
		
		return boxs;
	}
	
	protected void setUp() throws IOException {
		directory = new RAMDirectory();
		
		IndexWriter writer = new IndexWriter(directory,
				new WhitespaceAnalyzer(), true,
				IndexWriter.MaxFieldLength.UNLIMITED);

		setUpPlotter(16, 21);

		addData(writer);
		is =  new IndexSearcher(directory);

	}
	
	

	private void setUpPlotter(int base, int top) {

		for (; base <= top; base++) {
			ctps.add(new CartesianTierPlotter(base, project,
					CartesianTierPlotter.DEFALT_FIELD_PREFIX));
		}
	}
	
	private void addPoint(IndexWriter writer, String name, double lat, double lng) throws IOException{
	    
	    Document doc = new Document();
	    
	    doc.add(new Field("name", name,Field.Store.YES, Field.Index.ANALYZED));
	    
	    // convert the lat / long to lucene fields
	    doc.add(new Field(latField, NumericUtils.doubleToPrefixCoded(lat),Field.Store.YES, Field.Index.NOT_ANALYZED));
	    doc.add(new Field(lngField, NumericUtils.doubleToPrefixCoded(lng),Field.Store.YES, Field.Index.NOT_ANALYZED));
	    
	    // add a default meta field to make searching all documents easy 
	    doc.add(new Field("metafile", "doc",Field.Store.YES, Field.Index.ANALYZED));
	    
	    int ctpsize = ctps.size();
	    for (int i =0; i < ctpsize; i++){
	      CartesianTierPlotter ctp = ctps.get(i);
	      doc.add(new Field(ctp.getTierFieldName(), 
	          NumericUtils.doubleToPrefixCoded(ctp.getTierBoxId(lat,lng)),
	          Field.Store.YES, 
	          Field.Index.NOT_ANALYZED_NO_NORMS));
	      
	      if (ctp.getTierFieldName().equals("_tier_16"))
	    	  System.out.println(ctp.getTierBoxId(lat,lng));
	      
	      doc.add(new Field(geoHashPrefix, GeoHashUtils.encode(lat,lng), 
	    		  Field.Store.YES, 
	    		  Field.Index.NOT_ANALYZED_NO_NORMS));
	    }
	    writer.addDocument(doc);
	    
	  }
	
	public void addData(IndexWriter writer) throws IOException{
		addPoint(writer, "1", 37.421435292172944,-122.1847915649414);
		addPoint(writer, "2", 37.42552411384199,-122.1847915649414);
		addPoint(writer, "3", 37.42961293551104,-122.1847915649414);
		addPoint(writer, "4", 37.43370175718009,-122.1847915649414);
		addPoint(writer, "5", 37.43779057884913,-122.1847915649414);
		addPoint(writer, "6", 37.441879400518175,-122.1847915649414);
		addPoint(writer, "7", 37.445968222187226,-122.1847915649414);
		addPoint(writer, "8", 37.450057043856276,-122.1847915649414);
		addPoint(writer, "9", 37.45414586552532,-122.1847915649414);
		addPoint(writer, "10", 37.45823468719436,-122.1847915649414);
		addPoint(writer, "11", 37.421435292172944,-122.17620849609375);
		addPoint(writer, "12", 37.42552411384199,-122.17620849609375);
		addPoint(writer, "13", 37.42961293551104,-122.17620849609375);
		addPoint(writer, "14", 37.43370175718009,-122.17620849609375);
		addPoint(writer, "15", 37.43779057884913,-122.17620849609375);
		addPoint(writer, "16", 37.441879400518175,-122.17620849609375);
		addPoint(writer, "17", 37.445968222187226,-122.17620849609375);
		addPoint(writer, "18", 37.450057043856276,-122.17620849609375);
		addPoint(writer, "19", 37.45414586552532,-122.17620849609375);
		addPoint(writer, "20", 37.45823468719436,-122.17620849609375);
		addPoint(writer, "21", 37.421435292172944,-122.1676254272461);
		addPoint(writer, "22", 37.42552411384199,-122.1676254272461);
		addPoint(writer, "23", 37.42961293551104,-122.1676254272461);
		addPoint(writer, "24", 37.43370175718009,-122.1676254272461);
		addPoint(writer, "25", 37.43779057884913,-122.1676254272461);
		addPoint(writer, "26", 37.441879400518175,-122.1676254272461);
		addPoint(writer, "27", 37.445968222187226,-122.1676254272461);
		addPoint(writer, "28", 37.450057043856276,-122.1676254272461);
		addPoint(writer, "29", 37.45414586552532,-122.1676254272461);
		addPoint(writer, "30", 37.45823468719436,-122.1676254272461);
		addPoint(writer, "31", 37.421435292172944,-122.15904235839844);
		addPoint(writer, "32", 37.42552411384199,-122.15904235839844);
		addPoint(writer, "33", 37.42961293551104,-122.15904235839844);
		addPoint(writer, "34", 37.43370175718009,-122.15904235839844);
		addPoint(writer, "35", 37.43779057884913,-122.15904235839844);
		addPoint(writer, "36", 37.441879400518175,-122.15904235839844);
		addPoint(writer, "37", 37.445968222187226,-122.15904235839844);
		addPoint(writer, "38", 37.450057043856276,-122.15904235839844);
		addPoint(writer, "39", 37.45414586552532,-122.15904235839844);
		addPoint(writer, "40", 37.45823468719436,-122.15904235839844);
		addPoint(writer, "41", 37.421435292172944,-122.15045928955078);
		addPoint(writer, "42", 37.42552411384199,-122.15045928955078);
		addPoint(writer, "43", 37.42961293551104,-122.15045928955078);
		addPoint(writer, "44", 37.43370175718009,-122.15045928955078);
		addPoint(writer, "45", 37.43779057884913,-122.15045928955078);
		addPoint(writer, "46", 37.441879400518175,-122.15045928955078);
		addPoint(writer, "47", 37.445968222187226,-122.15045928955078);
		addPoint(writer, "48", 37.450057043856276,-122.15045928955078);
		addPoint(writer, "49", 37.45414586552532,-122.15045928955078);
		addPoint(writer, "50", 37.45823468719436,-122.15045928955078);
		addPoint(writer, "51", 37.421435292172944,-122.14187622070312);
		addPoint(writer, "52", 37.42552411384199,-122.14187622070312);
		addPoint(writer, "53", 37.42961293551104,-122.14187622070312);
		addPoint(writer, "54", 37.43370175718009,-122.14187622070312);
		addPoint(writer, "55", 37.43779057884913,-122.14187622070312);
		addPoint(writer, "56", 37.441879400518175,-122.14187622070312);
		addPoint(writer, "57", 37.445968222187226,-122.14187622070312);
		addPoint(writer, "58", 37.450057043856276,-122.14187622070312);
		addPoint(writer, "59", 37.45414586552532,-122.14187622070312);
		addPoint(writer, "60", 37.45823468719436,-122.14187622070312);
		addPoint(writer, "61", 37.421435292172944,-122.13329315185547);
		addPoint(writer, "62", 37.42552411384199,-122.13329315185547);
		addPoint(writer, "63", 37.42961293551104,-122.13329315185547);
		addPoint(writer, "64", 37.43370175718009,-122.13329315185547);
		addPoint(writer, "65", 37.43779057884913,-122.13329315185547);
		addPoint(writer, "66", 37.441879400518175,-122.13329315185547);
		addPoint(writer, "67", 37.445968222187226,-122.13329315185547);
		addPoint(writer, "68", 37.450057043856276,-122.13329315185547);
		addPoint(writer, "69", 37.45414586552532,-122.13329315185547);
		addPoint(writer, "70", 37.45823468719436,-122.13329315185547);
		addPoint(writer, "71", 37.421435292172944,-122.12471008300781);
		addPoint(writer, "72", 37.42552411384199,-122.12471008300781);
		addPoint(writer, "73", 37.42961293551104,-122.12471008300781);
		addPoint(writer, "74", 37.43370175718009,-122.12471008300781);
		addPoint(writer, "75", 37.43779057884913,-122.12471008300781);
		addPoint(writer, "76", 37.441879400518175,-122.12471008300781);
		addPoint(writer, "77", 37.445968222187226,-122.12471008300781);
		addPoint(writer, "78", 37.450057043856276,-122.12471008300781);
		addPoint(writer, "79", 37.45414586552532,-122.12471008300781);
		addPoint(writer, "80", 37.45823468719436,-122.12471008300781);
		addPoint(writer, "81", 37.421435292172944,-122.11612701416016);
		addPoint(writer, "82", 37.42552411384199,-122.11612701416016);
		addPoint(writer, "83", 37.42961293551104,-122.11612701416016);
		addPoint(writer, "84", 37.43370175718009,-122.11612701416016);
		addPoint(writer, "85", 37.43779057884913,-122.11612701416016);
		addPoint(writer, "86", 37.441879400518175,-122.11612701416016);
		addPoint(writer, "87", 37.445968222187226,-122.11612701416016);
		addPoint(writer, "88", 37.450057043856276,-122.11612701416016);
		addPoint(writer, "89", 37.45414586552532,-122.11612701416016);
		addPoint(writer, "90", 37.45823468719436,-122.11612701416016);
		addPoint(writer, "91", 37.421435292172944,-122.1075439453125);
		addPoint(writer, "92", 37.42552411384199,-122.1075439453125);
		addPoint(writer, "93", 37.42961293551104,-122.1075439453125);
		addPoint(writer, "94", 37.43370175718009,-122.1075439453125);
		addPoint(writer, "95", 37.43779057884913,-122.1075439453125);
		addPoint(writer, "96", 37.441879400518175,-122.1075439453125);
		addPoint(writer, "97", 37.445968222187226,-122.1075439453125);
		addPoint(writer, "98", 37.450057043856276,-122.1075439453125);
		addPoint(writer, "99", 37.45414586552532,-122.1075439453125);
		addPoint(writer, "100", 37.45823468719436,-122.1075439453125);

		writer.commit();
	    writer.close();
	}
	
	
	public IndexSearcher getIndexSearcher(){
		return is;
	}
	
}
