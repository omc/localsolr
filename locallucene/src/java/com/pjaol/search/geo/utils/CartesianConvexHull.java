package com.pjaol.search.geo.utils;

import java.util.List;

import com.pjaol.search.geo.utils.projections.CartesianTierPlotter;
import com.pjaol.search.geo.utils.projections.IProjector;
import com.pjaol.search.geo.utils.projections.SinusoidalProjector;

public class CartesianConvexHull extends CartesianLineString {

	public CartesianConvexHull(String tierId) {
		super(tierId);
		
	}

	public static void main(String[] args) {
		CartesianConvexHull cch = new CartesianConvexHull("_localTier15");
		IProjector ip = new SinusoidalProjector();
		CartesianTierPlotter ctp = new CartesianTierPlotter(15, ip);
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
	
		System.out.println(cch.area);
	}
	
	public void closeLoop(){
		if ( area.get(0) != area.get(area.size() -1)){
			addBox(area.get(0));
		}		
	}
	
	// TODO: provide a fill method
	
}
