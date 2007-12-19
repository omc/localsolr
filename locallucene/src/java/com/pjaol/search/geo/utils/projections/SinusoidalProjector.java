/**
 * * Copyright 2007 Patrick O'Leary 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific 
 * language governing permissions and limitations under the License.
 *
 */

package com.pjaol.search.geo.utils.projections;

/**
 * Based on Sinusoidal Projections
 * Project a latitude / longitude on a 2D cartisian map
 * 
 * @author pjaol
 *
 */
public class SinusoidalProjector implements IProjector {

	
	public String coordsAsString(double latitude, double longitude) {
		return null;
	}
	
	public double[] coords(double latitude, double longitude) {
		
		latitude = (longitude - 0 ) * Math.cos(latitude);
		double r[] = {latitude, longitude};
		return r;
		
	}
	
}
