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

import java.awt.geom.Rectangle2D;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.RangeFilter;
import org.apache.solr.util.NumberUtils;

import com.pjaol.lucene.search.SerialChainFilter;

public class DistanceQuery{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private BooleanQuery query = new BooleanQuery();
	public BoundaryBoxFilter latFilter;
	public BoundaryBoxFilter lngFilter;
	
	/**
	 * Create a distance Query or 2 filters based upon
	 * a boundary box range filter.
	 * Recommend using Filters latFilter lngFilter
	 * for the moment in conjunction with
	 * SerialChainFilter
	 * 
	 * @see SerialChainFilter
	 * @param lat
	 * @param lng
	 * @param miles
	 */
	public DistanceQuery (double lat, double lng, double miles){
		
		Rectangle2D box = DistanceUtils.getBoundary(lat, lng,miles);
		double x1 = box.getY(); //
		double y1 = box.getX(); //
		double x2 = box.getMaxY();
		double y2 = box.getMaxX();
		
	
		latFilter = new BoundaryBoxFilter("lat", NumberUtils.double2sortableStr(x1), NumberUtils.double2sortableStr(x2), true, true);
		lngFilter = new BoundaryBoxFilter("lng", NumberUtils.double2sortableStr(y1), NumberUtils.double2sortableStr(y2), true, true);
		
		query.add(new ConstantScoreQuery(latFilter),
				BooleanClause.Occur.MUST);
		
		query.add(new ConstantScoreQuery(lngFilter),
				BooleanClause.Occur.MUST);
		
	}
	
	public BooleanQuery getQuery(){
		return query;
	}
	
	public String toString() {
		return query.toString();
	}
}
