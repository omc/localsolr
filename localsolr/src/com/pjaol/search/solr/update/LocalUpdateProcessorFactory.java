/**
 * 
 */
package com.pjaol.search.solr.update;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.DOMUtil;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

import org.apache.solr.util.NumberUtils;
import org.w3c.dom.Node;


/**
 * {@link LocalUpdateProcessorFactory}
 * Required for CartesianTier indexing takes string parameters latField lngField
 * from the solrconfig node.
 * Example solrconfig.xml is :
 * <pre>
 * &lt;updateRequestProcessor&gt;
 *   &lt;factory name="standard" class="solr.ChainedUpdateProcessorFactory" default="true"&gt;
 *     &lt;chain class="com.pjaol.search.solr.update.LocalUpdateProcessorFactory"&gt;
 *       &lt;str name="latField"&gt;lat&lt;/str&gt;
 *       &lt;str name="lngField"&gt;lng&lt;/str&gt;
 *       &lt;int name="startTier"&gt;9&lt;/int&gt;
 *       &lt;int name="endTier"&gt;17&lt;/int&gt;
 *     &lt;/chain&gt;
 *     &lt;chain class="solr.LogUpdateProcessorFactory" &gt;
 *      &lt;!-- &lt;int name="maxNumToLog"&gt;100&lt;/int&gt; --&gt;
 *     &lt;/chain&gt;
 *     &lt;chain class="solr.RunUpdateProcessorFactory" /&gt;
 *   &lt;/factory&gt;
 * &lt;/updateRequestProcessor&gt;
 * </pre>
 * @author pjaol
 * 
 */
public class LocalUpdateProcessorFactory extends UpdateRequestProcessorFactory {

	String latField = "lat";

	String lngField = "lng";
	
	String tierPrefix = "_localTier";

	int cartesianStartTier = 6;

	int cartesianEndTier = 14;

	@Override
	public void init(NamedList args) {
		super.init(args);
		if (args != null) {
			//NamedList<Object> args = DOMUtil.childNodesToNamedList(node);
			SolrParams params = SolrParams.toSolrParams(args);
			latField = params.get("latField", latField);
			lngField = params.get("lngField", lngField);
			tierPrefix = params.get("tierPrefix", tierPrefix);
			cartesianStartTier = params.getInt("startTier", cartesianStartTier);
			cartesianEndTier = params.getInt("endTier", cartesianEndTier);
		}
	}

	@Override
	public UpdateRequestProcessor getInstance(SolrQueryRequest req,
			SolrQueryResponse rsp, UpdateRequestProcessor next) {

		LocalUpdaterProcessor lup = new LocalUpdaterProcessor(next);
		lup.setup(latField, lngField, tierPrefix,
				cartesianStartTier, cartesianEndTier, next);
		
		return lup;
	}
}

class LocalUpdaterProcessor extends UpdateRequestProcessor {

	IProjector projector = new SinusoidalProjector();

	int startTier, endTier;

	String latField, lngField, tierPrefix;

	UpdateRequestProcessor next;

	List<CartesianTierPlotter> plotters = new LinkedList<CartesianTierPlotter>();

	public LocalUpdaterProcessor (UpdateRequestProcessor next){
		super(next);
	}
	
	public void setup(String latField, String lngField, String tierPrefix,
			int startTier, int endTier, UpdateRequestProcessor next) {

		this.latField = latField;
		this.lngField = lngField;
		this.tierPrefix = tierPrefix;
		this.startTier = startTier;
		this.endTier = endTier;
		this.next = next;

		setupPlotters(startTier, endTier, tierPrefix);
	}

	public void setupPlotters(int startTier, int endTier, String tierPrefix) {

		for (int i = startTier; i < endTier; i++) {
			plotters.add(new CartesianTierPlotter(i, projector, tierPrefix));
		}

	}

	@Override
	public void finish() throws IOException {
		if (next != null)
			next.finish();
	}

	@Override
	public void processAdd(AddUpdateCommand cmd) throws IOException {
		SolrInputDocument doc = cmd.getSolrInputDocument();

		
		Object lat =  doc.getFieldValue(latField);
		Object lng =  doc.getFieldValue(lngField);
		
		Double dLat = 0.0, dLng= 0.0;
		
		if (lat instanceof String){
			dLat = new Double((String) lat);
		} else {
			dLat = (Double)lat;
		}
		
		if (lng instanceof String){
			dLng = new Double((String) lng);
		} else {
			dLng = (Double)lng;
		}
		
		if (lat != null && lng != null) {
			for (CartesianTierPlotter ctp : plotters) {

				doc.addField(ctp.getTierFieldName(), ctp.getTierBoxId(
						dLat, dLng));
			}
		}
		if (next != null)
			next.processAdd(cmd);
	}

	@Override
	public void processCommit(CommitUpdateCommand cmd) throws IOException {
		if (next != null)
			next.processCommit(cmd);

	}

	@Override
	public void processDelete(DeleteUpdateCommand cmd) throws IOException {
		if (next != null)
			next.processDelete(cmd);
	}

}
