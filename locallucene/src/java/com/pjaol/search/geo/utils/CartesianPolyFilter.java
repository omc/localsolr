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
package com.pjaol.search.geo.utils;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

import org.apache.lucene.search.RangeFilter;
import org.apache.solr.util.NumberUtils;

import com.pjaol.search.geo.utils.projections.CartesianTierPlotter;
import com.pjaol.search.geo.utils.projections.IProjector;
import com.pjaol.search.geo.utils.projections.SinusoidalProjector;

/**
 * @author pjaol
 *
 */
public class CartesianPolyFilter {

	private IProjector projector = new SinusoidalProjector();
	private Logger log = Logger.getLogger(getClass().getName());
	public RangeFilter boundaryBox (double latitude, double longitude, int miles){
		
		Rectangle2D box = DistanceUtils.getBoundary(latitude, longitude, miles);
		
		double latX = box.getY();
		double latY = box.getMaxY();
		
		double longX = box.getX();
		double longY = box.getMaxX();
		
		CartesianTierPlotter ctp = new CartesianTierPlotter(2, projector);
		int bestFit = ctp.bestFit(miles);
		
		log.fine("Best Fit is : " + bestFit);
		ctp = new CartesianTierPlotter(bestFit, projector);
		
		
		double beginAt = ctp.getTierBoxId(latX, longX);
		double endAt = ctp.getTierBoxId(latY, longY);
		String fieldName = ctp.getTierFieldName();
		
		log.fine("RangeFilter is ("+latX+","+longX+") "+"("+latY+","+longY+") "+fieldName+":["+beginAt +" TO "+ endAt+"]");
		
		RangeFilter f = new RangeFilter(fieldName, 
					NumberUtils.double2sortableStr(beginAt),
					NumberUtils.double2sortableStr(endAt),
					true, true);
		
		
		
		return f;
		
	}
	
}
