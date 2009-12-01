package com.pjaol.search;


import org.apache.lucene.spatial.tier.DistanceUtils;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.spatial.tier.projections.XYProjector;

public class TierBoundaries {
	
	CartesianTierPlotter ctp ;
	public int defaultLevel = 16;
	IProjector projector = new XYProjector();
	PointsD topLeft, topRight, bottomLeft, bottomRight;
	
	public PointsD getTopLeft() {
		return topLeft;
	}

	public void setTopLeft(PointsD topLeft) {
		this.topLeft = topLeft;
	}

	public PointsD getTopRight() {
		return topRight;
	}

	public void setTopRight(PointsD topRight) {
		this.topRight = topRight;
	}

	public PointsD getBottomLeft() {
		return bottomLeft;
	}

	public void setBottomLeft(PointsD bottomLeft) {
		this.bottomLeft = bottomLeft;
	}

	public PointsD getBottomRight() {
		return bottomRight;
	}

	public void setBottomRight(PointsD bottomRight) {
		this.bottomRight = bottomRight;
	}

	public static void main(String[] args) {
		TierBoundaries tb = new TierBoundaries();
		tb.getBoundaries(37.43629392840975,-122.13848663330079);
	}
	
	public TierBoundaries(){
		ctp = new CartesianTierPlotter(defaultLevel, projector, CartesianTierPlotter.DEFALT_FIELD_PREFIX);
		
	}
	
	public TierBoundaries(IProjector projector){
		this.projector = projector;
		ctp = new CartesianTierPlotter(defaultLevel, projector, CartesianTierPlotter.DEFALT_FIELD_PREFIX);
	}
	
	public TierBoundaries(CartesianTierPlotter ctp){
		this.ctp = ctp;
	}
	
	public void getBoundaries(double lat, double lng){
		
		double baseBoxId = ctp.getTierBoxId(lat, lng);
		double currentBoxLeft = baseBoxId, currentBoxRight = baseBoxId;
		double incVal = 0.000000001;
		double rightLng = lng, leftLng = lng;
		do {
			if (currentBoxLeft == baseBoxId){
				rightLng -= incVal;
				currentBoxLeft = ctp.getTierBoxId(lat, rightLng);
			}
			
			if (currentBoxRight == baseBoxId){
				leftLng += incVal;
				currentBoxRight = ctp.getTierBoxId(lat, leftLng);
			}
			
		}while (currentBoxLeft == baseBoxId || currentBoxRight == baseBoxId);

		double downLat = lat, upLat = lat;
		currentBoxLeft = baseBoxId;
		currentBoxRight = baseBoxId;
		do {
			if (currentBoxLeft == baseBoxId){
				downLat -= incVal;
				currentBoxLeft = ctp.getTierBoxId(downLat, lng);
			}
			
			if (currentBoxRight == baseBoxId){
				upLat += incVal;
				currentBoxRight = ctp.getTierBoxId(upLat, lng);
			}
			
		}while (currentBoxLeft == baseBoxId || currentBoxRight == baseBoxId);
		
//		System.out.println("Right : ("+lat+","+rightLng+")");
//		System.out.println("Left : ("+lat+","+leftLng+")");
//		
//		
//		
//		double distance = DistanceUtils.getInstance().getDistanceMi(lat, rightLng, lat, leftLng);
//		System.out.println("Distance: " + distance);
//		
//		System.out.println("Down : ("+downLat+","+lng+")");
//		System.out.println("Up : ("+upLat+","+lng+")");
//		
//		distance = DistanceUtils.getInstance().getDistanceMi(downLat, lng, upLat, lng);
//		System.out.println("Distance: " + distance);
//		
//		System.out.println("Corners");
//		System.out.println("(" + upLat+","+leftLng+")("+upLat+","+rightLng+")");
//		System.out.println("("+downLat+","+leftLng+")("+downLat+","+rightLng+")");
		
		topLeft = new PointsD(upLat, leftLng);
		topRight = new PointsD(upLat, rightLng);
		bottomLeft = new PointsD(downLat, leftLng);
		bottomRight = new PointsD(downLat, rightLng);
	}

	
	
	public String toKMLCoords() {
	
		return topLeft.toKMLCoord()+",0\n"
			   +topRight.toKMLCoord()+",0\n"
			   +bottomRight.toKMLCoord()+",0\n"
			   +bottomLeft.toKMLCoord()+",0\n"
			   +topLeft.toKMLCoord()+",0\n";
	}
	
	@Override
	public String toString() {
		
		return topLeft.toString()+"\n"
			   +topRight.toString()+"\n"
			   +bottomRight.toString()+"\n"
			   +bottomLeft.toString()+"\n"
			   +topLeft.toString()+"n";
	}
}
