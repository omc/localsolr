package com.pjaol.search;

public class RangeItems {

	double x,y;
	
	public RangeItems(double x, double y){
		this.x = x;
		this.y = y;
	}
	
	public double getLower(){
		
		if (x < y) 
			return x;
		return y;
	}
	
	
	public double getUpper(){
		if (x > y)
			return x;
		return y;
	}
	
}
