/**
 * 
 */
package com.pjaol.search.geo.utils;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Filter;

/**
 * @author pjaol
 *
 */
public class BoundaryBoxFilter extends Filter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String fieldName;
    private String lowerTerm;
    private String upperTerm;
    private boolean includeLower;
    private boolean includeUpper;
	
    private Map coords;
    
    /**
     * @param fieldName The field this range applies to
     * @param lowerTerm The lower bound on this range
     * @param upperTerm The upper bound on this range
     * @param includeLower Does this range include the lower bound?
     * @param includeUpper Does this range include the upper bound?
     * @throws IllegalArgumentException if both terms are null or if
     *  lowerTerm is null and includeLower is true (similar for upperTerm
     *  and includeUpper)
     */
    public BoundaryBoxFilter(String fieldName, String lowerTerm, String upperTerm,
                       boolean includeLower, boolean includeUpper) {
        this.fieldName = fieldName;
        this.lowerTerm = lowerTerm;
        this.upperTerm = upperTerm;
        this.includeLower = includeLower;
        this.includeUpper = includeUpper;
        
        if (null == lowerTerm && null == upperTerm) {
            throw new IllegalArgumentException
                ("At least one value must be non-null");
        }
        if (includeLower && null == lowerTerm) {
            throw new IllegalArgumentException
                ("The lower bound must be non-null to be inclusive");
        }
        if (includeUpper && null == upperTerm) {
            throw new IllegalArgumentException
                ("The upper bound must be non-null to be inclusive");
        }
    }
    
    
	 /**
     * Returns a BitSet with true for documents which should be
     * permitted in search results, and false for those that should
     * not.
     */
    public BitSet bits(IndexReader reader) throws IOException {
        BitSet bits = new BitSet(reader.maxDoc());
        TermEnum enumerator =
            (null != lowerTerm
             ? reader.terms(new Term(fieldName, lowerTerm))
             : reader.terms(new Term(fieldName,"")));
        
        coords = new HashMap(enumerator.docFreq());
        
        try {
            
            if (enumerator.term() == null) {
                return bits;
            }
            
            boolean checkLower = false;
            if (!includeLower) // make adjustments to set to exclusive
                checkLower = true;
        
            TermDocs termDocs = reader.termDocs();
            try {
                
                do {
                    Term term = enumerator.term();
                    if (term != null && term.field().equals(fieldName)) {
                        if (!checkLower || null==lowerTerm || term.text().compareTo(lowerTerm) > 0) {
                            checkLower = false;
                            if (upperTerm != null) {
                                int compare = upperTerm.compareTo(term.text());
                                /* if beyond the upper term, or is exclusive and
                                 * this is equal to the upper term, break out */
                                if ((compare < 0) ||
                                    (!includeUpper && compare==0)) {
                                    break;
                                }
                            }
                            /* we have a good term, find the docs */
                            
                            termDocs.seek(enumerator.term());
                            while (termDocs.next()) {
                                bits.set(termDocs.doc());
                                coords.put(termDocs.doc(), term.text());
                            }
                        }
                    } else {
                        break;
                    }
                }
                while (enumerator.next());
                
            } finally {
                termDocs.close();
            }
        } finally {
            enumerator.close();
        }

        return bits;
    }
    
    
    //return the coords hash map
    public Map getCoords() {
    	return coords;
    }

    public Object getCoord(int docid){
    	return coords.get(docid);
    }
    
    public void cleanUp () {
    	coords = null;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(fieldName);
        buffer.append(":");
        buffer.append(includeLower ? "[" : "{");
        if (null != lowerTerm) {
            buffer.append(lowerTerm);
        }
        buffer.append("-");
        if (null != upperTerm) {
            buffer.append(upperTerm);
        }
        buffer.append(includeUpper ? "]" : "}");
        return buffer.toString();
    }
    
}
