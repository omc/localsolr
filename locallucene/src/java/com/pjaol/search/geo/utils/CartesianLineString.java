package com.pjaol.search.geo.utils;

import java.util.ArrayList;
import java.util.Collections;
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
	double positionFix;
	
	public static void main(String[] args) {
		CartesianLineString cls = new CartesianLineString("_localTier15");
		IProjector ip = new SinusoidalProjector();
		CartesianTierPlotter ctp = new CartesianTierPlotter(15, ip);
		cls.setPlotter(ctp);
		cls.doQuickTests();
		
	}
	
	
	public CartesianLineString(String tierId) {
		super(tierId);
	}

	public void setPlotter(CartesianTierPlotter ctp){
		this.ctp = ctp;
		positionFix = 1 / ctp.getTierVerticalPosDivider();
	}
	
	@Override
	public void addBox(double boxId) {
		
		int sz = area.size(); 
		if (sz ==0)
			area.add(boxId);
		else{
			double id = (Double) area.get(sz -1 );
			//System.out.println("Got id "+ id );
			double[] aCoords = getXY(id);
			double[] bCoords = getXY(boxId);
//			System.out.println("x: "+aCoords[0]
//			                                 +"| y: "+ aCoords[1]
//			                                 +"| x1: "+ bCoords[0]
//			                                 +"| y1: "+ bCoords[1]);
			
			
			List<Double> steps = addVertex(aCoords[0], aCoords[1], bCoords[0], bCoords[1]);
			area.addAll(steps.subList(1, steps.size()));
			
		}
			
	}
	
	private double[] getXY(double boxId){
	
		// I hate this with a passion
		// decimal positioning sucks, trying to deal with it using
		// strings :-( 
		String bid = new Double(boxId).toString();
		//System.out.println(bid);
		String [] xy = bid.split("\\.");
		
		return new double[] {new Double(xy[0]), new Double(xy[1])};
	}


	private List<Double> addVertex (double x, double y, double x1, double y1){
		//System.out.println("("+x+","+y+") -> ("+x1+","+y1+")");
		
		List<Double> boxes = new ArrayList<Double>();
		double xDiff = (x -x1);
		double yDiff = (y - y1);
		
		double ySlope = xDiff / yDiff;
		double xSlope = yDiff / xDiff;
		
		boolean xUp = (x < x1)? true: false; // 
		boolean yUp = (y < y1)? true: false;
		
		//System.out.println(xSlope + " -- "+ ySlope);
		// TODO: over thinking this, there's an easier way to step through this
		if (Math.abs(xSlope) < Math.abs(ySlope)){
			if (xUp){
				//System.out.println("xUp");
				boxes = takeSteps(x, x1, y, xSlope, false);
			}else{
				//System.out.println("xDown");
				boxes = takeSteps(x, x1, y, -xSlope, false);
			}
		} else {
			// tested and works
			if (yUp){
				//System.out.println("yUp");
				boxes = takeSteps(y,y1, x, ySlope, true);
			}else{
				//System.out.println("yDown");
				boxes = takeSteps(y,y1, x, -ySlope, true);
			}
		}

		return boxes;
	}
	
	private List<Double> takeSteps(double start, double end, double coord, double slope, boolean invert){
		List<Double> boxes = new ArrayList<Double>();
		
		//System.out.println(start+"->"+end+" :nc "+coord+" /slope "+slope+"|tvp "+ctp.getTierVerticalPosDivider()+"/"+invert);
		double tierVerticalPosDivider = ctp.getTierVerticalPosDivider();
		int sign = -1;
		int xSign = -1;
		int ySign = -1;
		if (start < end) // walking left or right?
			sign = 1;
		
		if (coord > 0) // y leading value sign
			ySign = 1;
		
		if (start > 0) // x leading value sign
			xSign =1;
		
		// TODO: over thinking this, there's an easier way to step through this
		
		// num of steps = Math.max(Math.abs(start), Math.abs(end)) - Math.min(Math.abs(start), Math.abs(end))
		
		double steps = Math.max(Math.abs(start), Math.abs(end)) 
									- Math.min(Math.abs(start), Math.abs(end));
		
		double currentStart = start;
		
		for (int i =0 ; i <= steps; i++){
			double nCoord = roundNear(coord);
			double box;
			
			if (invert){
				double yPos = (ySign * (currentStart/ tierVerticalPosDivider));
				
				box = nCoord +yPos;
				//System.out.println(box+"|nc "+nCoord+"|yPos "+yPos);
			} else {
				
				double yPos = (xSign * (nCoord / tierVerticalPosDivider) );
				box = currentStart+ yPos;
				//System.out.println(box+"|nc "+nCoord+"|yPos "+yPos);
			}
			
			coord += slope;
			//System.out.println("\t"+coord);
			currentStart +=  sign;
			
			boxes.add(box);
		}
	
		return boxes;
	}
	
 
	private double roundNear (double d){

		if (d < 0)
			return Math.ceil(d);
		
		return Math.floor(d);
	}

	
	private List<Double> arrayToList(double[] args){
		List<Double> a = new ArrayList<Double>();
		for (double b: args)
			a.add(b);
		
		return a;
	}
	
	// TODO: move to a unit test
	private void doQuickTests(){
		List<Double> test = new ArrayList<Double>();
		List<Double> t2 = new ArrayList<Double>();
		
	
		// yUp
		test = arrayToList(new double[]{-10.00001, -10.00002, -10.00003, -11.00004, -11.00005, -12.00006, -12.00007, -13.00008, -13.00009, -14.0001, -14.00011, -14.00012, -15.00013, -15.00014, -16.00015, -16.00016, -17.00017, -17.00018, -18.00019, -18.0002, -18.00021, -19.00022, -19.00023, -20.00024, -20.00025, -21.00026, -21.00027, -22.00028, -22.00029, -22.0003, -23.00031, -23.00032, -24.00033, -24.00034, -25.00035, -25.00036, -26.00037, -26.00038, -26.00039, -27.0004, -27.00041, -28.00042, -28.00043, -29.00044, -29.00045, -30.00046, -30.00047, -31.00048, -31.00049, -31.0005, -32.00051, -32.00052, -33.00053, -33.00054, -34.00055, -34.00056, -35.00057, -35.00058, -35.00059, -36.0006, -36.00061, -37.00062, -37.00063, -38.00064, -38.00065, -39.00066, -39.00067, -39.00068, -40.00069, -40.0007, -41.00071, -41.00072, -42.00073, -42.00074, -43.00075, -43.00076, -43.00077});
		t2 = addVertex(-10,1, -44, 77);
		
		if ( test.hashCode() == t2.hashCode())
			System.out.println("Test 1 OK");
		else
			System.err.println("Test 1 FAIL" + test +"!="+t2);
		
		// yUp
		area = new ArrayList<Double>();
		test = arrayToList(new double[]{-1.00001, -1.00002, -2.00003, -2.00004, -3.00005, -4.00006});
		t2 = addVertex(-1, 1, -4, 6);
		
		if ( test.hashCode() == t2.hashCode())
			System.out.println("Test 2 OK");
		else
			System.err.println("Test 2 FAIL" + test +"!="+t2);
		
		// yDown
		area = new ArrayList<Double>();
		test = arrayToList(new double[]{-4.00006, -4.00005, -5.00004, -6.00003});
		t2 = addVertex(-4, 6, -6, 3);
		if ( test.hashCode() == t2.hashCode())
			System.out.println("Test 3 OK");
		else
			System.err.println("Test 3 FAIL" + test +"!="+t2);
		
		// xDown
		area = new ArrayList<Double>();
		test = arrayToList(new double[]{-1.00006, -2.00006, -3.00006, -4.00006});
		t2 = addVertex(-1,6, -4, 6);
		if ( test.hashCode() == t2.hashCode())
			System.out.println("Test 4 OK");
		else
			System.err.println("Test 4 FAIL" + test +"!="+t2);
		
		// xUp
		area = new ArrayList<Double>();
		test = arrayToList(new double[] {-6.00003, -5.00003, -4.00003, -3.00003, -2.00004});
		t2 = addVertex(-6, 3 , -2,4);
		if ( test.hashCode() == t2.hashCode())
			System.out.println("Test 5 OK");
		else
			System.err.println("Test 5 FAIL" + test +"!="+t2);
	}
}
