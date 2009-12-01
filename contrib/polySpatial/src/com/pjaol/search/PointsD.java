package com.pjaol.search;

public class PointsD {

	double x, y;

	public PointsD(Double x, Double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	
	@Override
	public String toString() {
	
		return x+","+y;
	}
	
	
	public String toKMLCoord() {
	
		return y+","+x;
	}
	
	@Override
	public boolean equals(Object obj) {
		PointsD other = (PointsD)obj;
		return (other.getX() == this.x && other.getY() == this.y);
	}
	
	public int hashCode(){
		
		int result = new Double(x).hashCode();
		result ^= 31* new Double(y).hashCode();
		
		return result;
	}
}
