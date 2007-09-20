package com.pjaol.search.solr;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.solr.core.SolrCore;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.request.SimpleFacets;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.handler.StandardRequestHandler;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.util.HighlightingUtils;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.util.SolrPluginUtils;
import org.apache.solr.common.util.StrUtils;

import com.pjaol.lucene.search.SerialChainFilter;
import com.pjaol.search.geo.utils.DistanceFilter;
import com.pjaol.search.geo.utils.DistanceQuery;
import com.pjaol.search.geo.utils.DistanceSortSource;

public class LocalRequestHandler implements SolrRequestHandler, SolrInfoMBean {

	  // statistics
	  // TODO: should we bother synchronizing these, or is an off-by-one error
	  // acceptable every million requests or so?
	  long numRequests;
	  long numErrors;
	  SolrParams defaults;
	  SolrParams appends;
	  SolrParams invariants;

	  
	  /** shorten the class references for utilities */
	  private static class U extends SolrPluginUtils {
	    /* :NOOP */
	  }

	  public void init(NamedList args) {
	    Object o = args.get("defaults");
	    if (o != null && o instanceof NamedList) {
	      defaults = SolrParams.toSolrParams((NamedList)o);
	    }
	    o = args.get("appends");
	    if (o != null && o instanceof NamedList) {
	      appends = SolrParams.toSolrParams((NamedList)o);
	    }
	    o = args.get("invariants");
	    if (o != null && o instanceof NamedList) {
	      invariants = SolrParams.toSolrParams((NamedList)o);
	    }
	    
	  }

	  public void handleRequest(SolrQueryRequest req, SolrQueryResponse rsp) {
	    numRequests++;
	    
	    try {
	      U.setDefaults(req,defaults,appends,invariants);
	      SolrParams p = req.getParams();
	      String sreq = p.get(CommonParams.Q);

	      String defaultField = p.get(CommonParams.DF);

	      String lat = p.get("lat");
	      String lng = p.get("long");
	      
	      String radius = p.get("radius");
	      
	      
	      
	      // find fieldnames to return (fieldlist)
	      String fl = p.get(CommonParams.FL);
	      int flags = 0; 
	      if (fl != null) {
	        flags |= U.setReturnFields(fl, rsp);
	      }

	      if (sreq==null) throw new SolrException(ErrorCode.BAD_REQUEST,"Missing queryString");
	      List<String> commands = StrUtils.splitSmart(sreq,';');

	      String qs = commands.size() >= 1 ? commands.get(0) : "";
	      Query query = QueryParsing.parseQuery(qs, defaultField, p, req.getSchema());

	      // If the first non-query, non-filter command is a simple sort on an indexed field, then
	      // we can use the Lucene sort ability.
	      Sort sort = null;
	      if (commands.size() >= 2) {
	        QueryParsing.SortSpec sortSpec = QueryParsing.parseSort(commands.get(1), req.getSchema());
	        if (sortSpec != null) {
	          sort = sortSpec.getSort();
	          // ignore the count for now... it's currently only controlled by start & limit on req
	          // count = sortSpec.getCount();
	        }
	      }

	      DocListAndSet results = new DocListAndSet();
	      NamedList facetInfo = null;
	      List<Query> filters = U.parseFilterQueries(req);
	      SolrIndexSearcher s = req.getSearcher();
	      DocSet f = null;
	      DistanceFilter filter = null;
	      
	      if (lat != null && lng != null && radius != null) {
	    	  
	    	  double dlat = new Double(lat).doubleValue();
	    	  double dlng = new Double(lng).doubleValue();
	    	  double dradius = new Double(radius).doubleValue();
	    	  
	    	  DistanceQuery dq = new DistanceQuery(dlat,dlng,dradius);
	    	  filter = new DistanceFilter(dlat, dlng, dradius, dq.latFilter, dq.lngFilter);
	    	  
	    	  SerialChainFilter scf = new SerialChainFilter(new Filter[] {dq.latFilter, dq.lngFilter, filter} ,
	  				new int[] {SerialChainFilter.AND,
	  						   SerialChainFilter.AND,
	  						   SerialChainFilter.SERIALAND});
	    	  CachingWrapperFilter cwf = new CachingWrapperFilter(scf);
	    	  
	    	  f = s.convertFilter(cwf);
	    	  
	    	  DistanceSortSource dsort = new DistanceSortSource(filter);
	    	  sort = new Sort(new SortField("foo", dsort));
	    	
	      }
	      
	      
	      
	      if (p.getBool(FacetParams.FACET,false)) {
	    	System.out.println("calling here"+ + f.size());
	        results = s.getDocListAndSet(query, f, sort,
	                                     p.getInt(CommonParams.START,0), p.getInt(CommonParams.ROWS,10),
	                                     flags);
	        facetInfo = getFacetInfo(req, rsp, results.docSet);
	      } else {
	    	
	    	
//	        results.docList = s.getDocList(query, filters, sort,
//	                                       p.getInt(SolrParams.START,0), p.getInt(SolrParams.ROWS,10),
//	                                       flags);
	    	results.docList = s.getDocList(query, f, sort, p.getInt(CommonParams.START,0), p.getInt(CommonParams.ROWS,10));
	    	
	      }

	      if (filter != null ){
	    	  rsp.add("distances", filter.getDistances());
	      }
	      
	      // pre-fetch returned documents
	      U.optimizePreFetchDocs(results.docList, query, req, rsp);
	      
	      rsp.add("response",results.docList);
	      
	      if (null != facetInfo) rsp.add("facet_counts", facetInfo);

	      try {
	        NamedList dbg = U.doStandardDebug(req, qs, query, results.docList);
	        if (null != dbg) {
	          if (null != filters) {
	            dbg.add("filter_queries",req.getParams().getParams(CommonParams.FQ));
	            List<String> fqs = new ArrayList<String>(filters.size());
	            for (Query fq : filters) {
	              fqs.add(QueryParsing.toString(fq, req.getSchema()));
	            }
	            dbg.add("parsed_filter_queries",fqs);
	          }
	          rsp.add("debug", dbg);
	        }
	      } catch (Exception e) {
	        SolrException.logOnce(SolrCore.log, "Exception during debug", e);
	        rsp.add("exception_during_debug", SolrException.toStr(e));
	      }

	      NamedList sumData = HighlightingUtils.doHighlighting(
	        results.docList, query, req, new String[]{defaultField});
	      if(sumData != null)
	        rsp.add("highlighting", sumData);

	    } catch (SolrException e) {
	      rsp.setException(e);
	      numErrors++;
	      return;
	    } catch (Exception e) {
	      SolrException.log(SolrCore.log,e);
	      rsp.setException(e);
	      numErrors++;
	      return;
	    }
	  }

	  /**
	   * Fetches information about Facets for this request.
	   *
	   * Subclasses may with to override this method to provide more 
	   * advanced faceting behavior.
	   * @see SimpleFacets#getFacetCounts
	   */
	  protected NamedList getFacetInfo(SolrQueryRequest req, 
	                                   SolrQueryResponse rsp, 
	                                   DocSet mainSet) {

	    SimpleFacets f = new SimpleFacets(req.getSearcher(), 
	                                      mainSet, 
	                                      req.getParams());
	    return f.getFacetCounts();
	  }



	  //////////////////////// SolrInfoMBeans methods //////////////////////


	  public String getName() {
	    return StandardRequestHandler.class.getName();
	  }

	  public String getVersion() {
	    return SolrCore.version;
	  }

	  public String getDescription() {
	    return "The standard Solr request handler";
	  }

	  public Category getCategory() {
	    return Category.QUERYHANDLER;
	  }

	  public String getSourceId() {
	    return "$Id: LocalRequestHandler.java,v 1.3 2007-09-20 19:36:13 pjaol Exp $";
	  }

	  public String getSource() {
	    return "$URL: $";
	  }

	  public URL[] getDocs() {
	    return null;
	  }

	  public NamedList getStatistics() {
	    NamedList lst = new NamedList();
	    lst.add("requests", numRequests);
	    lst.add("errors", numErrors);
	    return lst;
	  }

	
}
