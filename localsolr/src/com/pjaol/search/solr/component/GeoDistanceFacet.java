package com.pjaol.search.solr.component;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;

import org.apache.solr.request.SimpleFacets;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocSet;

public class GeoDistanceFacet extends SimpleFacets {

	private NamedList distances = new NamedList();
	public GeoDistanceFacet(SolrQueryRequest req, DocSet docs, SolrParams params) {
		super(req, docs, params);
		
	}

	public void setDistances(NamedList distances){
		if (distances != null)
			this.distances = distances;
	}
	
	/**
   * Looks at various Params to determing if any simple Facet Constraint count
   * computations are desired.
   *
   * @see #getFacetQueryCounts
   * @see #getFacetFieldCounts
   * @see #getFacetDateCounts
   * @see FacetParams#FACET
   * @return a NamedList of Facet Count info or null
   */
  public NamedList getFacetCounts() {

    // if someone called this method, benefit of the doubt: assume true
    if (!params.getBool(FacetParams.FACET,true))
      return null;

    NamedList res = new SimpleOrderedMap();
    try {

      res.add("facet_queries", getFacetQueryCounts());
      res.add("facet_fields", getFacetFieldCounts());
      res.add("facet_dates", getFacetDateCounts());
      res.add("facet_geo_distances", distances);
    } catch (Exception e) {
      SolrException.logOnce(SolrCore.log, "Exception during facet counts", e);
      res.add("exception", SolrException.toStr(e));
    }
    return res;
  }
}
