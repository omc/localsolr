package com.pjaol.search.solr.component;

import java.io.IOException;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.SimpleFacets;

public class LocalSolrFacetComponent extends FacetComponent {

	
	/**
   * Actually run the query
   * @param rb
   */
  @Override
  public void process(ResponseBuilder rb) throws IOException
  {
    if (rb.doFacets) {
      SolrParams params = rb.req.getParams();
      GeoDistanceFacet f = new GeoDistanceFacet(rb.req,
              rb.getResults().docSet,
              params);
      NamedList distances = (NamedList) rb.req.getContext().get(LocalSolrParams.geo_facet_context);
      f.setDistances(distances);
      // TODO ???? add this directly to the response, or to the builder?
      rb.rsp.add( "facet_counts", f.getFacetCounts() );
    }
  }
	
}
