package com.pjaol.search.solr.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrCache;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SortSpec;
import org.apache.solr.update.DocumentBuilder;
import org.apache.solr.util.SolrPluginUtils;

import com.pjaol.search.geo.utils.DistanceQuery;
import com.pjaol.search.geo.utils.DistanceSortSource;
import com.pjaol.search.geo.utils.DistanceUtils;
import com.pjaol.search.solr.LocalSolrRequestHandler;
import com.pjaol.search.solr.LocalSolrSortParser;

/**
 * {@link LocalSolrQueryComponent}
 * Can be loaded through {@link LocalSolrRequestHandler}
 * @see LocalSolrRequestHandler
 * @author pjaol
 *
 */
public class LocalSolrQueryComponent extends SearchComponent {

	private String DistanceQuery = "DistanceQuery";

	private String DistanceCache = "distanceCache";

	private Logger log = Logger.getLogger(getClass().getName());
	private String latField = "lat";
	private String lngField = "lng";
	
	public LocalSolrQueryComponent() {
		
	}
	
	public LocalSolrQueryComponent (String lat, String lng) {
		
		if ( lat != null && lng != null){
			log.info("Setting latField to "+lat +" setting lngField to "+ lng);
			latField = lat;
			lngField = lng;
		}
	}

	@Override
	public void init(NamedList initArgs) {
		this.latField = (String) initArgs.get("latField");
		this.lngField = (String) initArgs.get("lngField");
	}
	
	@Override
	public void prepare(SolrQueryRequest req, SolrQueryResponse rsp)
			throws IOException, ParseException {
		ResponseBuilder builder = SearchHandler.getResponseBuilder(req);
		SolrParams params = req.getParams();

		// Set field flags
		String fl = params.get(CommonParams.FL);
		int fieldFlags = 0;

		if (fl != null) {
			fieldFlags |= SolrPluginUtils.setReturnFields(fl, rsp);
		}
		builder.setFieldFlags(fieldFlags);

		builder.setQueryString(params.get(CommonParams.Q));

		String lat = params.get("lat");
		String lng = params.get("long");

		String radius = params.get("radius");

		DistanceSortSource dsort = null;

		DistanceQuery dq = null;

		if (lat != null && lng != null && radius != null) {

			double dlat = new Double(lat).doubleValue();
			double dlng = new Double(lng).doubleValue();
			double dradius = new Double(radius).doubleValue();

			// TODO pull latitude /longitude from configuration 
			dq = new DistanceQuery(dlat, dlng, dradius, latField, lngField, true);

			dsort = new DistanceSortSource(dq.distanceFilter);
		}

		// Parse sort
		String sortStr = params.get(CommonParams.SORT);
		if (sortStr == null) {
			// TODO? should we disable the ';' syntax with config?
			// legacy mode, where sreq is query;sort
			List<String> commands = StrUtils.splitSmart(builder
					.getQueryString(), ';');
			if (commands.size() == 2) {
				// TODO? add a deprication warning to the response header
				builder.setQueryString(commands.get(0));
				sortStr = commands.get(1);
			} else if (commands.size() == 1) {
				// This is need to support the case where someone sends:
				// "q=query;"
				builder.setQueryString(commands.get(0));
			} else if (commands.size() > 2) {
				throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
						"If you want to use multiple ';' in the query, use the 'sort' param.");
			}
		}

		if (sortStr != null) {
			SortSpec lss = new LocalSolrSortParser().parseSort(sortStr, req
					.getSchema(), dsort);
			if (lss != null) {
				builder.setSortSpec(lss);
			}

		}

		// parse the query from the 'q' parameter (sort has been striped)
		String defaultField = params.get(CommonParams.DF);
		builder.setQuery(QueryParsing.parseQuery(builder.getQueryString(),
				defaultField, params, req.getSchema()));

		// parse filters
		List<Query> filters = SolrPluginUtils.parseFilterQueries(req);
		if (filters != null) {
			filters.add(dq.getQuery());

		} else {
			filters = new ArrayList<Query>();
			filters.add(dq.getQuery());

		}

		builder.setFilters(filters);

		req.getContext().put(DistanceQuery, dq);

//		Map<String, String> federatedExtras = new HashMap<String, String>();
//		federatedExtras.put(CommonParams.QT, params.get(CommonParams.QT));
//		federatedExtras.put(CommonParams.WT, params.get(CommonParams.WT));

	}

	@Override
	public void process(SolrQueryRequest req, SolrQueryResponse rsp)
			throws IOException {
		ResponseBuilder builder = SearchHandler.getResponseBuilder(req);
		SolrIndexSearcher searcher = req.getSearcher();
		
		SolrParams params = req.getParams();
		String lat = params.get("lat");
		String lng = params.get("long");
		
		
		SolrCache distanceCache = searcher.getCache(DistanceCache);

		DocSet f = null;
		DistanceQuery dq = (DistanceQuery) req.getContext().get(DistanceQuery);

		boolean cachedDistances = false;

		Filter optimizedDistanceFilter = dq.getFilter(builder.getQuery());

		if (distanceCache != null) {

			// Does this region have it's geography cached?
			Map<Integer, Double> distances = (Map<Integer, Double>) distanceCache
					.get(dq.distanceFilter);

			if (distances != null) {
				dq.distanceFilter.setDistances(distances);
				cachedDistances = true;
			} else {
				// try and get cache for a query based geography filter.
				distances = (Map<Integer, Double>) distanceCache
						.get(optimizedDistanceFilter);
				if (distances != null) {
					dq.distanceFilter.setDistances(distances);
					cachedDistances = true;
				}
			}

		}

		if (!cachedDistances) {
			// Run the optimized geography filter
			f = searcher.convertFilter(optimizedDistanceFilter);
			if (distanceCache != null) {
				distanceCache.put(optimizedDistanceFilter, dq.distanceFilter
						.getDistances());
			}
		}

		Sort sort = null;
		if (builder.getSortSpec() != null) {
			sort = builder.getSortSpec().getSort();
		}

		if (builder.isNeedDocSet()) {

			if (!cachedDistances) {
				// use a standard query
				log.fine("Standard query...");

				builder
						.setResults(searcher
								.getDocListAndSet(builder.getQuery(), f, sort,
										params.getInt(CommonParams.START, 0),
										params.getInt(CommonParams.ROWS, 10),
										builder.getFieldFlags()));
			} else {
				// use a cached query
				log.fine("Cached query....");
				builder.setResults(searcher
						.getDocListAndSet(builder.getQuery(), dq.getQuery(),
								sort, params.getInt(CommonParams.START, 0),
								params.getInt(CommonParams.ROWS, 10), builder
										.getFieldFlags()));
			}

		} else {

			log.fine("DocList query....");
			DocListAndSet results = new DocListAndSet();
			if (!cachedDistances) {
				log.fine("Using reqular...");
				
				results.docList = searcher.getDocList(builder.getQuery(), f,
						sort, params.getInt(CommonParams.START, 0), params
								.getInt(CommonParams.ROWS, 10));
			} else {
				log.fine("Using cached.....");
				results.docList = searcher
						.getDocList(builder.getQuery(), builder.getFilters(),
								sort, params.getInt(CommonParams.START, 0),
								params.getInt(CommonParams.ROWS, 10), builder
										.getFieldFlags());
			}
			builder.setResults(results);
		}

		// pre-fetch returned documents
		SolrPluginUtils.optimizePreFetchDocs(builder.getResults().docList,
				builder.getQuery(), req, rsp);

		Map distances = new HashMap();
		if (dq.distanceFilter != null) {
			// if (dsort != null)
			// dsort.cleanUp();

			// builder.sort = null;
			distances =  dq.distanceFilter.getDistances();

		}
		SolrDocumentList sdoclist = mergeResultsDistances(builder.getResults().docList, 
				distances, searcher, rsp.getReturnFields(),
				new Double(lat).doubleValue(), 
				new Double(lng).doubleValue());

		log.finer("Adding SolrDocumentList "+ sdoclist.size());
		rsp.add("response", sdoclist);


		// Add distance sorted response for merging later...
		// Part of the MainQueryPhase response

		String sortStr = req.getParams().get(CommonParams.SORT);
		String fsv = req.getParams().get("fsv"); // ResponseBuilder.FIELD_SORT_VALUES);
		if ((fsv != null) && (sortStr != null)
				&& (sortStr.startsWith("geo_distance"))) {

			NamedList responseSortFields = new NamedList();

			DocIterator it = builder.getResults().docList.iterator();
			while (it.hasNext()) {
				NamedList sortFields = new NamedList();
				int docId = it.nextDoc();
				Document doc = req.getSearcher().doc(docId);

				String docUniqKey = SolrCore.getSolrCore().getSchema().getUniqueKeyField(doc).stringValue();
						;

				Double distance = (Double) dq.distanceFilter.getDistance(docId);
				sortFields.add("geo_distance", distance);

				responseSortFields.add(docUniqKey, sortFields);
			}

			rsp.add("response_sort_fields", responseSortFields);

		}

		
	}

	
	private SolrDocumentList mergeResultsDistances (DocList docs, Map distances, SolrIndexSearcher searcher, Set<String> fields, double latitude, double longitude){
		SolrDocumentList sdoclist = new SolrDocumentList();
		sdoclist.setNumFound(docs.matches());
		sdoclist.setMaxScore(docs.maxScore());
		sdoclist.setStart(docs.offset());
		
		DocIterator dit = docs.iterator();

		while(dit.hasNext()){
			int docid = dit.nextDoc();
			try {
				SolrDocument sd = luceneDocToSolrDoc(docid, searcher, fields);
				if(distances != null)
					
					sd.addField("geo_distance", distances.get(docid));
				else {
					
					double docLat = (Double)sd.getFieldValue(latField);
					double docLong = (Double)sd.getFieldValue(lngField);
					double distance =DistanceUtils.getDistanceMi(docLat, docLong, latitude, longitude);
					
					sd.addField("geo_distance", distance);	
				}
				
				// this may be removed if XMLWriter gets patched to
				// include score from doc iterator in solrdoclist
				if(docs.hasScores()){
					sd.addField("score", dit.score());
				} else {
					sd.addField("score", 0.0f);
				}
				sdoclist.add(sd);
				
				
			} catch (IOException e) {
				// TODO possible slip or should we fail?
				e.printStackTrace();
			}
			
		}
		
		
		return sdoclist;
	}
	
	
	public SolrDocument luceneDocToSolrDoc (int docid, SolrIndexSearcher searcher, Set fields) throws IOException{
		Document luceneDoc = searcher.doc(docid, fields);
		
		SolrDocument sdoc = new SolrDocument();
		DocumentBuilder db = new DocumentBuilder(searcher.getSchema());
		sdoc = db.loadStoredFields(sdoc, luceneDoc);
		return sdoc;
	}
	
	/*
	 * Solr InfoBean
	 */

	@Override
	public String getDescription() {

		return "LocalSolrQueryComponent: $File: $";
	}

	@Override
	public String getSource() {

		return "$File: $";
	}

	@Override
	public String getSourceId() {

		return "$Id: $";
	}

	@Override
	public String getVersion() {

		return "$Version: $";
	}
}
