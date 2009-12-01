package com.pjaol.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;

public class CartesianConvexHull extends CartesianLineString {

	public CartesianConvexHull(String tierId) {
		super(tierId);
		
	}

	public static void main(String[] args) {
		CartesianConvexHull cch = new CartesianConvexHull("_localTier15");
		IProjector ip = new SinusoidalProjector();
		CartesianTierPlotter ctp = new CartesianTierPlotter(15, ip, CartesianTierPlotter.DEFALT_FIELD_PREFIX);
		cch.setPlotter(ctp);
		
		double [][] coords = { {-1, 1} , {-4, 6} , {-6, 3}, {-2,4}};
		double tierVerticalPositionDivider = ctp.getTierVerticalPosDivider();
		
		for(double[] cd : coords){
		
			int sign = -1;
			if (cd[0] > 0)
				sign = 1;
			
			// generate a box id
			double id = cd[0] + ( sign * (cd[1] / tierVerticalPositionDivider) );
			System.out.println("==>"+id);
			
			cch.addBox(id);
		}
	
		System.out.println(cch.getArea());
	}
	
	public void closeLoop(){
		if ( getArea().get(0) != getArea().get(getArea().size() -1)){
			addBox(getArea().get(0));
		}		
	}
	
	// TODO: provide a fill method
	
	
	public List<RangeItems> getFillAsRanges(){
		List<RangeItems> ranges = new ArrayList<RangeItems>();
		
		double base =0.0, marj =0.0, lastBid = 0.0;
		List<Double>area = getArea();
		
		// sort the box id's
		Collections.sort(area);
		
		
		for(Double boxId: area){
		
			// if the major x changes, then base to previous boxid is a range
			// e.g.
			// 1.001, 1.002, 1.003, 2.002, 2.003, 2.004, 3.001, 3.002, 3.003
			// = 1.001 TO 1.003
			//   2.002 TO 2.004
			//   3.001 TO 3.003
			
			double cmarj = boxId - (boxId % 1);
			
			if(base == 0.0){
				base = boxId;
				marj = cmarj;
			}
			
			if (cmarj != marj){
			
				ranges.add(new RangeItems(base, lastBid));
				marj = cmarj;
				base = boxId;
			}
			lastBid = boxId;
		}
		
		if(lastBid != base){
			ranges.add(new RangeItems(base, lastBid));
		}
		
		return ranges;
	}
	
	
	public String asTextQuery(){
		String query = new String();
		
		for(RangeItems r: getFillAsRanges()){
			String rq = ctp.getTierFieldName()+":["+r.getLower()+" TO "+r.getUpper()+"]";
			if (query.equals(""))
				query += rq;
			else
				query += " OR "+ rq;
			
			//System.out.println(rq);
		}
		
		return query;
	}
	
}
