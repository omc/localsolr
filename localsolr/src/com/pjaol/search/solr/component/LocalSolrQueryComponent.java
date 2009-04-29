package com.pjaol.search.solr.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.spatial.tier.DistanceFieldComparatorSource;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.spatial.tier.DistanceSortSource;
import org.apache.lucene.spatial.tier.DistanceUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SortSpec;
import org.apache.solr.update.DocumentBuilder;
import org.apache.solr.util.SolrPluginUtils;


import com.pjaol.search.solr.LocalSolrSortParser;

/**
 * {@link LocalSolrQueryComponent} Can be loaded through
 * {@link LocalSolrRequestHandler}
 * 
 * Can now be instantiated in solrconfig.xml as
 * <pre>
 * &lt;!-- local lucene request handler -->
 * &lt;searchComponent name="localsolr"     class="com.pjaol.search.solr.component.LocalSolrQueryComponent" />
 * &lt;searchComponent name="geofacet"      class="com.pjaol.search.solr.component.LocalSolrFacetComponent"/>
 * &lt;requestHandler name="geo" class="org.apache.solr.handler.component.SearchHandler">
 *   &lt;arr name="components">
 *     &lt;str>localsolr&lt;/str>
 *     &lt;str>facet&lt;/str> &lt;!-- or geofacet see com.pjaol.search.solr.component.LocalSolrFacetComponent -->
 *     &lt;str>mlt&lt;/str>
 *     &lt;str>highlight&lt;/str>
 *     &lt;str>debug&lt;/str>
 *   &lt;/arr>
 * &lt;/requestHandler>
 * </pre>
 * 
 * @see LocalSolrRequestHandler
 * @author pjaol
 * 
 */
public class LocalSolrQueryComponent extends SearchComponent {

	private String DistanceQuery = "DistanceQuery";

	private Logger log = Logger.getLogger(getClass().getName());
	private String latField = "lat";
	private String lngField = "lng";

	public LocalSolrQueryComponent() {

	}

	public LocalSolrQueryComponent(String lat, String lng) {

		if (lat != null && lng != null) {
			log.info("Setting latField to " + lat + " setting lngField to "
					+ lng);
			latField = lat;
			lngField = lng;
		}
	}

	/**
	 * Can be initialized in the solrconfig.xml with custom lat / long field names
	 * &lt;searchComponent name="localsolr"     class="com.pjaol.search.solr.component.LocalSolrQueryComponent" />
	 *     &lt;str name="latField">lat&lt;/str>
   *     &lt;str name="lngField">lng&lt;/str>
   * &lt;/searchComponent>
	 */
	@Override
	public void init(NamedList initArgs) {

		String lat = (String) initArgs.get("latField");
		String lng = (String) initArgs.get("lngField");

		if (lat != null && lng != null) {
			log.info("Setting latField to " + lat + " setting lngField to "
					+ lng);
			latField = lat;
			lngField = lng;
		}
	}

	public void prepare(ResponseBuilder builder) throws IOException {

		SolrQueryRequest req = builder.req;
		SolrQueryResponse rsp = builder.rsp;
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

		DistanceQueryBuilder dq = null;

		if (lat != null && lng != null && radius != null) {

			double dlat = new Double(lat).doubleValue();
			double dlng = new Double(lng).doubleValue();
			double dradius = new Double(radius).doubleValue();

			// TODO pull latitude /longitude from configuration
			dq = new DistanceQueryBuilder(dlat, dlng, dradius, latField, lngField,"_localTier",	true);

		}

		// parse the query from the 'q' parameter (sort has been striped)
		String defaultField = params.get(CommonParams.DF);
		builder.setQuery(QueryParsing.parseQuery(builder.getQueryString(),
				defaultField, params, req.getSchema()));

		// parse filters
		List<Query> filters = builder.getFilters();
		String[] fqs = req.getParams().getParams(
				org.apache.solr.common.params.CommonParams.FQ);
		if (fqs != null && fqs.length != 0) {

			if (filters == null) {
				filters = new ArrayList<Query>();
				builder.setFilters(filters);
			}
			for (String fq : fqs) {
				if (fq != null && fq.trim().length() != 0) {
					QParser fqp = null;
					try {
						fqp = QParser.getParser(fq, null, req);
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						filters.add(fqp.getQuery());
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		builder.setFilters(filters);

		req.getContext().put(DistanceQuery, dq);

	}

	@Override
	public void process(ResponseBuilder builder) throws IOException {
		SolrQueryRequest req = builder.req;
		SolrQueryResponse rsp = builder.rsp;
		SolrIndexSearcher searcher = req.getSearcher();

		SolrParams params = req.getParams();
		String lat = params.get("lat");
		String lng = params.get("long");

		
		// simply return id's
		String ids = params.get("ids");
		if (ids != null) {
			SchemaField idField = req.getSchema().getUniqueKeyField();
			String[] idArr = ids.split(","); // TODO... handle escaping
			int[] luceneIds = new int[idArr.length];
			int docs = 0;
			for (int i = 0; i < idArr.length; i++) {
				int id = req.getSearcher().getFirstMatch(
						new Term(idField.getName(), idField.getType()
								.toInternal(idArr[i])));

				if (id >= 0)
					luceneIds[docs++] = id;
			}

			DocListAndSet res = new DocListAndSet();
			res.docList = new DocSlice(0, docs, luceneIds, null, docs, 0);
			builder.setResults(res);

			log.fine("Adding SolrDocumentList from id's " + ids);

			SolrDocumentList sdoclist = mergeResultsDistances(builder
					.getResults().docList, null, searcher, rsp
					.getReturnFields(), new Double(lat).doubleValue(),
					new Double(lng).doubleValue());

			rsp.add("response", sdoclist);
			return;
		}

		DistanceQueryBuilder dq = (DistanceQueryBuilder) req.getContext().get(DistanceQuery);

		//Filter optimizedDistanceFilter = dq.getFilter(builder.getQuery());
		List<Query> filters = builder.getFilters();
		
		if (filters != null) {
			filters.add(dq.getQuery(builder.getQuery()));

		} else {
			filters = new ArrayList<Query>();
			filters.add(dq.getQuery(builder.getQuery()));

		}
		
		
		Map<Integer, Double> distances = null;

		// Run the optimized geography filter
		//f = searcher.convertFilter(optimizedDistanceFilter);
		
		DistanceSortSource dsort = null;
		dsort = new DistanceSortSource(dq.distanceFilter);

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

		Sort sort = null;
		if (builder.getSortSpec() != null) {
			sort = builder.getSortSpec().getSort();
		}

		if (builder.isNeedDocSet()) {

			// use a standard query
			log.fine("Standard query...");

			builder.setResults(searcher.getDocListAndSet(builder.getQuery(),
																									filters,
																									sort,
																									params.getInt(CommonParams.START, 0),
																									params.getInt(CommonParams.ROWS, 10), 
																									builder.getFieldFlags()));
			

		} else {

			log.fine("DocList query....");
			DocListAndSet results = new DocListAndSet();

			results.docList = searcher.getDocList(builder.getQuery(), 
																						filters, 
																						sort,
																						params.getInt(CommonParams.START, 0), 
																						params.getInt(CommonParams.ROWS, 10),
																						builder.getFieldFlags());
			builder.setResults(results);

		}

		if (distances == null)
			distances = dq.distanceFilter.getDistances();

		// pre-fetch returned documents
		SolrPluginUtils.optimizePreFetchDocs(builder.getResults().docList,
				builder.getQuery(), req, rsp);

		SolrDocumentList sdoclist = mergeResultsDistances(
				builder.getResults().docList, distances, searcher, rsp
						.getReturnFields(), new Double(lat).doubleValue(),
				new Double(lng).doubleValue());

		log.finer("Adding SolrDocumentList " + sdoclist.size());
		rsp.add("response", sdoclist);

		// Add distance sorted response for merging later...
		// Part of the MainQueryPhase response

		boolean fsv = req.getParams().getBool(
				ResponseBuilder.FIELD_SORT_VALUES, false);

		// field sort values used for sharding / distributed searching
		if (fsv) {
			// provide a sort_value in the response
			// do i really need a comparator if there is a
			// fieldable object with an internal - external representation ?
			SortField[] sortFields = sort == null ? new SortField[] { SortField.FIELD_SCORE }
					: sort.getSort();
			ScoreDoc sd = new ScoreDoc(0, 1.0f); // won't work for
			// comparators that look
			// at the score
			NamedList<ArrayList<Object>> sortVals = new NamedList<ArrayList<Object>>();

			for (SortField sortField : sortFields) {
				int type = sortField.getType();
				if (type == SortField.SCORE || type == SortField.DOC)
					continue;

				String fieldname = sortField.getField();
				FieldType ft = fieldname == null ? null : req.getSchema()
						.getFieldTypeNoEx(fieldname);

				DocList docList = builder.getResults().docList;
				ArrayList<Object> vals = new ArrayList<Object>(docList.size());
				DocIterator it = builder.getResults().docList.iterator();

				int docPosition = 0;
				while (it.hasNext()) {
					sd.doc = it.nextDoc();

					if (type != SortField.STRING) {
						// assume this is only used for shard-ing
						// thus field value should be internal representation of
						// the object
						try {
							if (type == SortField.CUSTOM
									&& (!fieldname.equals("geo_distance"))) {
								// assume it's a double, as there's a bug in
								// sdouble type
								vals.add(ft.toInternal(new Double(
										(Double) sdoclist.get(docPosition)
												.getFieldValue(fieldname))
										.toString()));
							} else {
								vals.add(ft.toInternal((String) sdoclist.get(
										docPosition).getFieldValue(fieldname)));
							}
						} catch (Exception e) {
							vals.add(sdoclist.get(docPosition).getFieldValue(
									fieldname));
						}
					} else {
						vals.add(sdoclist.get(docPosition).getFieldValue(
								fieldname));
					}
					docPosition++;
				}
				sortVals.add(fieldname, vals);
			}
			rsp.add("sort_values", sortVals);
		}

		
		// handle custom distance facet here rather than
		// passing all distances into the request context
		if (params.getBool(LocalSolrParams.geo_facet, false)){
			
			NamedList<Integer> geo_distances_facets = facet_distances(params,builder.getResults().docSet, distances);
			// place in request context to let GeoDistanceFacet
			// manage the placement in the facet list
			req.getContext().put(LocalSolrParams.geo_facet_context, geo_distances_facets);
		}
		
		if (dq.distanceFilter != null) {
			dsort.cleanUp();
			sort = null;
			distances = null;
//			optimizedDistanceFilter = null;
//			f = null;

		}

	}

	private SolrDocumentList mergeResultsDistances(DocList docs, Map<Integer, Double> distances,
			SolrIndexSearcher searcher, Set<String> fields, double latitude,
			double longitude) {
		SolrDocumentList sdoclist = new SolrDocumentList();
		sdoclist.setNumFound(docs.matches());
		sdoclist.setMaxScore(docs.maxScore());
		sdoclist.setStart(docs.offset());

		DocIterator dit = docs.iterator();
		//System.out.println( distances.size()+ ": "+distances.keySet());
		while (dit.hasNext()) {
			int docid = dit.nextDoc();
			try {
				SolrDocument sd = luceneDocToSolrDoc(docid, searcher, fields);
				if (distances != null) {
					sd.addField("geo_distance", new String(distances.get(docid)
							.toString()).toString());
				} else {

					double docLat = (Double) sd.getFieldValue(latField);
					double docLong = (Double) sd.getFieldValue(lngField);
					double distance = DistanceUtils.getInstance().getDistanceMi(docLat,
							docLong, latitude, longitude);

					sd.addField("geo_distance", distance);
				}

				// this may be removed if XMLWriter gets patched to
				// include score from doc iterator in solrdoclist
				if (docs.hasScores()) {
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

	public SolrDocument luceneDocToSolrDoc(int docid,
			SolrIndexSearcher searcher, Set<String> fields) throws IOException {
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
	
	
	
	/*
	 * Facet distances and provide a round method using facet.geo_distance.mod
	 */
	public NamedList<Integer> facet_distances(SolrParams params, DocSet docs, Map<Integer, Double> distances){
		NamedList<Integer> results = new NamedList<Integer>();
		double dradius = new Double(params.get("radius")).doubleValue();
		
		float modifer = params.getFloat(LocalSolrParams.geo_facet_mod, 1f);
		String type = params.get(LocalSolrParams.geo_facet_type, "count");
		String direction = params.get(LocalSolrParams.geo_facet_dir, "asc");
		int facet_count = params.getInt(LocalSolrParams.geo_facet_count, 100);
		String facet_buckets = params.get(LocalSolrParams.geo_facet_buckets);
		
		boolean buckets = false;
		String [] bucketArray = null;
		if (facet_buckets != null){
			buckets = true;
			bucketArray = facet_buckets.split(",");
		}
		
		boolean inclusive = params.getBool(LocalSolrParams.geo_facet_inclusive, false);
		
		if (facet_count <= 0)
			facet_count = Integer.MAX_VALUE;
		
		int stype = DistanceFacetHolder.COUNT;
		int sdirection = DistanceFacetHolder.ASC;
		
		if (type.equalsIgnoreCase("geo_distance"))
			stype = DistanceFacetHolder.DISTANCE;
		
		if (direction.equalsIgnoreCase("desc"))
			sdirection = DistanceFacetHolder.DESC;
		
		
		List<DistanceFacetHolder> distanceHolder = new ArrayList<DistanceFacetHolder>();
		Map<Double, Integer> position = new HashMap<Double, Integer>();
		List<DistanceFacetHolder> distanceInclusiveHolder = new ArrayList<DistanceFacetHolder>();
		Map<Double, Integer> distanceInclusiveCounter = new HashMap<Double, Integer>();
		
		int pos = 0;
		
		List<Double> sortedDistances = new ArrayList<Double>();
		DocIterator docIt = docs.iterator();
		// bucket and count the distances
		
		while(docIt.hasNext()){
			int docId = docIt.nextDoc();
			
		
			double di = distances.get(docId);
			if (buckets){
				sortedDistances.add(di);
				
			}
			// simple modulus round up 
			// e.g. 
			// mod = 0.5 , distance = 0.75
			// curD = 0.75 + (0.5 - (0.75 % 0.5)) 
			//  ''  = 0.75 + (0.5 - (0.25))
			//  ''  = 1
			
			Double curD = di + (modifer - (di % modifer));
			
			if (di > dradius)
				continue;
			
			if (position.containsKey(curD)){
				 int idx = position.get(curD);
				 distanceHolder.get(idx).incCount();
				 if (inclusive)
					 distanceInclusiveHolder.get(idx).incCount();
				 
			} else {
				
				distanceHolder.add(new DistanceFacetHolder(curD, stype, sdirection));
				if (inclusive)
					distanceInclusiveHolder.add(new DistanceFacetHolder(curD, 
																			DistanceFacetHolder.DISTANCE, 
																			DistanceFacetHolder.DESC));
				position.put(curD, pos);
				pos++;
			}
		}
		int amount = 0;
		Collections.sort(distanceHolder);
		
		if (inclusive){
			Collections.sort(distanceInclusiveHolder);
			int count = 0;
			for(DistanceFacetHolder dfh: distanceInclusiveHolder){
				count +=dfh.count;
				distanceInclusiveCounter.put(dfh.distance, count);
			}
		}
		
		if(buckets){
			Collections.sort(sortedDistances);
			double[] dbsA = new double[bucketArray.length +1];
			int i=0;
			// create the buckets array as doubles
			for(String dbs : bucketArray){
				
				dbsA[i] = new Double(dbs).doubleValue();
				i++;
			}
			
			i=0;
			int counter = 0;
			double currentBucket = 0;
			
			// iterate through all the distances placing
			// them in the appropriate buckets until radius is exceeded
			for(double ds: sortedDistances){
				
				if (ds > currentBucket || // distance exceeds bucket
						counter >= (sortedDistances.size() -1) ){ // last iteration
					
					while ((i < dbsA.length) && (ds > currentBucket)) {	
						
						if (ds > currentBucket)
							results.add(new Double(currentBucket).toString(), counter); // write the current bucket
						
						currentBucket = dbsA[i];
						i++;
					} 
					
					if (i >= dbsA.length){
						
					// out of buckets, time to finish
						results.add(new Double(dradius).toString(),  sortedDistances.size());
						break;
					}	
				}
				counter++; // natural numbers needed
			}
			
			
		} else {

			for (DistanceFacetHolder dfh : distanceHolder) {

				if (amount <= facet_count) {
					if (inclusive)
						results.add(dfh.distance.toString(), distanceInclusiveCounter
								.get(dfh.distance));
					else
						results.add(dfh.distance.toString(), dfh.count);
				} else
					break;

				amount++;
			}
		}
		
		return results;
	}
	
	/*
	 * Method for holding a collection of distances based on a key value
	 */
	class DistanceFacetHolder implements Comparable{
		Double distance;
		int count = 1; // use natural number
		int sortD = 0;
		int sortType = 0;
		
		final static int ASC =0;
		final static int DESC =1;
		
		final static int DISTANCE = 0;
		final static int COUNT = 1;
		
		public DistanceFacetHolder(Double curD, int type, int direction) {
			this.distance = curD;
			this.sortType = type;
			this.sortD = direction;
		}

		public void incCount(){
			this.count++;
		}
		
		public void incCount(int by){
			this.count += by;
		}
		
		public int compareTo(Object arg0) {
			DistanceFacetHolder b = (DistanceFacetHolder) arg0;
			int result;
			
			// distance asc
			if (sortType == DISTANCE){
				
				if (this.distance > b.distance)
					result = -1;
				else if (this.distance < b.distance)
					result = 1;
				else 
					result = 0; 
				
			} else {
						// count asc default
				if (this.count > b.count)
					result =  -1;
				else if(this.count < b.count)
					result = 1;
				else 
					result = 0;
			}
			
			if (sortD == DESC) // if to be inverted
				result *= -1;
			
			return result;
		}
		
	}
}
