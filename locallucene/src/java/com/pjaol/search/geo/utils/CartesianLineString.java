package com.pjaol.search.geo.utils;

import java.util.ArrayList;
import java.util.List;


import com.pjaol.search.geo.utils.projections.CartesianTierPlotter;
import com.pjaol.search.geo.utils.projections.IProjector;
import com.pjaol.search.geo.utils.projections.SinusoidalProjector;

/**
 * CartesianLineString
 * Add boxId's as away points, and this will 
 * generate the boxes in between as a Shape area, which can then 
 * be used in the CartesianPolyFilter
 * 
 * Experimental not tested 
 * @author pjaol
 * @deprecated experimental 
 */
public class CartesianLineString extends Shape{

	CartesianTierPlotter ctp ;
	
	public static void main(String[] args) {
		CartesianLineString cls = new CartesianLineString("_localTier15");
		IProjector ip = new SinusoidalProjector();
		CartesianTierPlotter ctp = new CartesianTierPlotter(15, ip);
		cls.setPlotter(ctp);
		
		cls.addVertex(-10,1, -44, 77);
	}
	
	
	public CartesianLineString(String tierId) {
		super(tierId);
	}

	public void setPlotter(CartesianTierPlotter ctp){
		this.ctp = ctp;
	}
	
	@Override
	public void addBox(double boxId) {
		
		int sz = area.size(); 
		if (sz ==0)
			area.add(boxId);
		else{
			double id = (Double) area.get(sz);
			
			double[] aCoords = getXY(id);
			double[] bCoords = getXY(boxId);
			
			area.addAll(addVertex(aCoords[0], aCoords[1], bCoords[0], bCoords[1]));
		}
			
	}
	private double[] getXY(double boxId){
	
		double y = boxId % 1;
		double x = boxId - y;
		double tierVertical = ctp.getTierVerticalPosDivider();
		y *= tierVertical;
		
		return new double[] {x, y};
	}


	private List<Double> addVertex (double x, double y, double x1, double y1){
		List<Double> boxes = new ArrayList();
		double xDiff = (x -x1);
		double yDiff = (y - y1);
		
		double ySlope = xDiff / yDiff;
		double xSlope = yDiff / xDiff;
		
		boolean xUp = (x < x1)? true: false; // 
		boolean yUp = (y < y1)? true: false;
		
		//System.out.println(xSlope + " -- "+ ySlope);
		
		if (Math.abs(xSlope) < Math.abs(ySlope)){
			if (xUp)
				boxes = takeSteps(x, x1, y, xSlope, false);
			else
				boxes = takeSteps(x1, x, y1, xSlope, false);
		} else {
			if (yUp)
				boxes = takeSteps(y,y1, x, ySlope, true);
			else
				boxes = takeSteps(y1,y, x1, ySlope, true);
		}

		return boxes;
	}
	
	private List<Double> takeSteps(double start, double end, double coord, double slope, boolean invert){
		List<Double> boxes = new ArrayList<Double>();
		
		//System.out.println(start+"->"+end+" :"+coord+" /"+slope+"|"+ctp.getTierVerticalPosDivider());
		double tierVerticalPosDivider = ctp.getTierVerticalPosDivider();
		int sign = -1;
		if (invert && coord >= 0)
			sign = 1;
		else if (!invert && sign >= 0)
			sign = 1;
		
		for(; start <= end; start++){
			double nCoord = roundNear(coord);
			double box;
			if (invert){
				box = nCoord +((start/ tierVerticalPosDivider) * sign);
				System.out.println(box);
				
			}else{
				box = start+ ( (nCoord / tierVerticalPosDivider) * sign);
				System.out.println(box);

			}
			coord += slope;
			
			boxes.add(box);
		}
		
		return boxes;
	}
	
	// if box is in either upper 10 or lower 10% move to next box 
	private double roundNear (double d){
		double uprecision = 0.91;
		double dif = d % 1;
		if (dif > uprecision)
			return Math.ceil(d);
		
		return Math.floor(d);
	}



}
