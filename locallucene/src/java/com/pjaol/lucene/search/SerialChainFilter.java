/**
 *Copyright 2007 Patrick O'Leary 
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
package com.pjaol.lucene.search;

import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;

/**
 * 
 * Provide a serial chain filter, passing the bitset in with the
 * index reader to each of the filters in an ordered fashion.
 * 
 * Based off chain filter, but will some improvements to allow a narrowed down
 * filtering. Traditional filter required iteration through an IndexReader.
 * 
 * By implementing the ISerialChainFilter class, you can create a bits(IndexReader reader, BitSet bits)
 * @see com.pjaol.lucene.search.ISerialChainFilter
 * 
 * 
 * @author Patrick O'Leary
 *
 */
public class SerialChainFilter extends Filter {

	
	/**
	 * $Id: SerialChainFilter.java,v 1.1 2007-10-12 18:45:12 pjaol Exp $
	 */
	private static final long serialVersionUID = 1L;
	private Filter chain[];
	public static final int SERIALAND = 1;
	public static final int SERIALOR = 2;
	public static final int AND = 3;	// regular filters may be used first
	public static final int OR = 4;		// regular filters may be used first
	public static final int DEFAULT = SERIALOR;
	
	private int actionType[];
	
	public SerialChainFilter(Filter chain[]){
		this.chain = chain;
		this.actionType = new int[] {DEFAULT};
	}
	
	public SerialChainFilter(Filter chain[], int actionType[]){
		this.chain= chain;
		this.actionType = actionType;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.lucene.search.Filter#bits(org.apache.lucene.index.IndexReader)
	 */
	@Override
	public BitSet bits(IndexReader reader) throws CorruptIndexException, IOException {
		
		BitSet bits = new BitSet(reader.maxDoc());
		int chainSize = chain.length;
		int actionSize = actionType.length;
		int i = 0;
		
		/**
		 * taken from ChainedFilter, first and on an empty bitset results in 0
		 */
		if (actionType[i] == AND){
			 try {
				bits = (BitSet) chain[i].bits(reader).clone();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	         ++i;
		}
		
		for( ; i < chainSize; i++) {
		
			int action = (i < actionSize)? actionType[i]: DEFAULT;
		
			switch (action){
			
			case (SERIALAND):
				try {
						bits.and(((ISerialChainFilter) chain[i]).bits(reader, bits));
					} catch (CorruptIndexException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				break;
			case (SERIALOR):
				try {
						bits.or(((ISerialChainFilter) chain[i]).bits(reader,bits));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				break;
			case (AND):
				bits.and(chain[i].bits(reader));
				break;
			case (OR):
				bits.and(chain[i].bits(reader));
				break;
			
			}
	
		}
		return bits;
	}

}
