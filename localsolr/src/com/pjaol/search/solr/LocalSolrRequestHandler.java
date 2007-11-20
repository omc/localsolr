package com.pjaol.search.solr;

import java.util.ArrayList;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.SearchHandler;
import org.apache.solr.handler.component.DebugComponent;
import org.apache.solr.handler.component.FacetComponent;
import org.apache.solr.handler.component.HighlightComponent;
import org.apache.solr.handler.component.MoreLikeThisComponent;
import org.apache.solr.handler.component.SearchComponent;

import com.pjaol.search.solr.component.LocalSolrQueryComponent;

public class LocalSolrRequestHandler extends SearchHandler {

	@Override
	public void initComponents(NamedList args) {

		components = new ArrayList<SearchComponent>(5);
		components.add(new LocalSolrQueryComponent());
		components.add( new FacetComponent() );
	    components.add( new MoreLikeThisComponent() );
	    components.add( new HighlightComponent() );
	    components.add( new DebugComponent() );
	}

}
