/**
 * Copyright 2007 Patrick O'Leary 
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

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;

import org.apache.solr.util.NumberUtils;

import com.pjaol.lucene.search.ISerialChainFilter;
import com.pjaol.search.geo.utils.DistanceHandler.precision;


public class DistanceFilter extends ISerialChainFilter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private double distance;
	private double lat;
	private double lng;
	private precision precise;
	
	
	private BoundaryBoxFilter latFilter;
	private BoundaryBoxFilter lngFilter;
	
	// no type safety for generic key
	private Map distances;
	
	/**
	 * Provide a distance filter based from a center point with a radius
	 * in miles
	 * @param lat
	 * @param lng
	 * @param miles
	 */
	public DistanceFilter(double lat, double lng, double miles){
		distance = miles;
		this.lat = lat;
		this.lng = lng;
		
	}
	
	public DistanceFilter(double lat, double lng, double miles, BoundaryBoxFilter bblatfilter, BoundaryBoxFilter bblngfilter){
		
		distance = miles;
		this.lat = lat;
		this.lng = lng;
		this.latFilter = bblatfilter;
		this.lngFilter = bblngfilter;
	}
	
	
	public Map getDistances(){
		return distances;
	}
	
	public Object getDistance(int docid){
		return distances.get(docid);
	}
	
	@Override
	public BitSet bits(IndexReader reader) throws IOException {
		
		int maxdocs = reader.numDocs();
		BitSet bits = new BitSet(maxdocs);
		
		setPrecision(maxdocs);
		WeakHashMap cdistance = new WeakHashMap(maxdocs);
		distances = new HashMap(maxdocs);
		for (int i = 0 ; i < maxdocs; i++) {
			
			Document doc = reader.document(i);
			
			double x = NumberUtils.SortableStr2double(doc.get("lat"));
			double y = NumberUtils.SortableStr2double(doc.get("lng"));
			
			// round off lat / longs if necessary
			x = DistanceHandler.getPrecision(x, precise);
			y = DistanceHandler.getPrecision(y, precise);
			
			String ck = new Double(x).toString()+","+new Double(y).toString();
			Double cachedDistance = (Double)cdistance.get(ck);
			
			
			double d;
			
			if(cachedDistance != null){
				d = cachedDistance.doubleValue();
			} else {
				d = DistanceUtils.getDistanceMi(lat, lng, x, y);
				cdistance.put(ck, d);
			}
			distances.put(i, d);
			
			if (d < distance){
				bits.set(i);
			}
			
		}
		
		return bits;
	}

	
	@Override
	public BitSet bits(IndexReader reader, BitSet bits) throws Exception {
		
		int size = bits.cardinality();
		BitSet result = new BitSet(size);
		int i = 0;
		setPrecision(size);
		HashMap cdistance = new HashMap(size);
		
		
		if (lngFilter == null || latFilter == null) {
			// should not be here!
			// DistanceFilter was initialized without a boundary box pass
			throw new Exception("DistanceFilter not initialized with serial chain correctly");
		
		}
			
		distances = new HashMap(size);
		
		long start = System.currentTimeMillis();
		while (i >= 0){
			i = bits.nextSetBit(i);
			if (i < 0)
				return result;
			
			double x,y;
			
			// if we have a completed
			// filter chain, lat / lngs can be retrived from 
			// memory rather than document base.
			String sx = (String)latFilter.getCoord(i);
			String sy = (String)lngFilter.getCoord(i);
			
			x = NumberUtils.SortableStr2double(sx);
			y = NumberUtils.SortableStr2double(sy);
				
			
			
			// round off lat / longs if necessary
			x = DistanceHandler.getPrecision(x, precise);
			y = DistanceHandler.getPrecision(y, precise);
			
			String ck = sx+","+sy;
			Double cachedDistance = (Double)cdistance.get(ck);
			double d;
			
			if(cachedDistance != null){
				d = cachedDistance.doubleValue();
				
			} else {
				d = DistanceUtils.getDistanceMi(lat, lng, x, y);
				cdistance.put(ck, d);
			}
			
			distances.put(i, d);
				
			if (d < distance){
				result.set(i);
			}
			i = bits.nextSetBit(i+1);
		}
		
		long end = System.currentTimeMillis();
		System.out.println("Time taken : "+ (end - start) + 
				" results : "+ distances.size() + 
				" cached : "+ cdistance.size());
	
		latFilter.cleanUp();
		lngFilter.cleanUp();
		cdistance = null;
		
		return result;
	}

	
	private void setPrecision(int maxDocs){
		precise = precision.EXACT;
		
		if (maxDocs > 1000) {
			precise = precision.TWENTYFEET;
		}
		
		if (maxDocs > 10000){
			precise = precision.TWOHUNDREDFEET;
		}
	}
	
}
