/**
 * 
 */
package com.pjaol.search.solr.update;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.document.Document;
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

import com.pjaol.search.geo.utils.projections.CartesianTierPlotter;
import com.pjaol.search.geo.utils.projections.IProjector;
import com.pjaol.search.geo.utils.projections.SinusoidalProjector;

/**
 * @author pjaol
 * 
 */
public class LocalUpdateProcessorFactory extends UpdateRequestProcessorFactory {

	String latField = "lat";

	String lngField = "lng";

	int cartesianStartTier = 6;

	int cartesianEndTier = 14;

	@Override
	public void init(SolrCore core, Node node) {
		super.init(core, node);
		if (node != null) {
			NamedList<Object> args = DOMUtil.childNodesToNamedList(node);
			SolrParams params = SolrParams.toSolrParams(args);
			latField = params.get("latField", latField);
			lngField = params.get("lngField", lngField);
			cartesianStartTier = params.getInt("startTier", cartesianStartTier);
			cartesianEndTier = params.getInt("endTier", cartesianEndTier);
		}
	}

	@Override
	public UpdateRequestProcessor getInstance(SolrQueryRequest req,
			SolrQueryResponse rsp, UpdateRequestProcessor next) {

		return new LocalUpdaterProcessor(latField, lngField,
				cartesianStartTier, cartesianEndTier, next);
	}
}

class LocalUpdaterProcessor extends UpdateRequestProcessor {

	IProjector projector = new SinusoidalProjector();

	int startTier, endTier;

	String latField, lngField;

	UpdateRequestProcessor next;

	List<CartesianTierPlotter> plotters = new LinkedList<CartesianTierPlotter>();

	public LocalUpdaterProcessor(String latField, String lngField,
			int startTier, int endTier, UpdateRequestProcessor next) {

		this.latField = latField;
		this.lngField = lngField;
		this.startTier = startTier;
		this.endTier = endTier;
		this.next = next;

		setupPlotters(startTier, endTier);
	}

	public void setupPlotters(int startTier, int endTier) {

		for (int i = startTier; i < endTier; i++) {
			plotters.add(new CartesianTierPlotter(i, projector));
		}

	}

	@Override
	public void finish() throws IOException {
		if (next != null)
			next.finish();
	}

	@Override
	public void processAdd(AddUpdateCommand cmd) throws IOException {
		// TODO Auto-generated method stub
		SolrInputDocument doc = cmd.getSolrInputDocument();

		String lat = (String) doc.getFieldValue("lat");
		String lng = (String) doc.getFieldValue("lng");
		log.fine("Adding lat/lngs: "+ lat +", "+lng);
		
		if (lat != null && lng != null) {
			for (CartesianTierPlotter ctp : plotters) {

				doc.addField(ctp.getTierFieldName(), ctp.getTierBoxId(
						new Double(lat), new Double(lng)));
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
