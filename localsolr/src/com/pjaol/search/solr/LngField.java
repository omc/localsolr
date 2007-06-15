/**
 ** Copyright 2007 Patrick O'Leary 
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
package com.pjaol.search.solr;

import java.io.IOException;

import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.SortField;
import org.apache.solr.request.TextResponseWriter;
import org.apache.solr.request.XMLWriter;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.SchemaField;

import com.pjaol.search.geo.utils.DistanceUtils;

/**
 * @author pjaol
 * 
 */
public class LngField extends FieldType {

	@Override
	public SortField getSortField(SchemaField field, boolean top) {

		return getStringSort(field, top);
	}

	@Override
	public void write(XMLWriter xmlWriter, String name, Fieldable f)
			throws IOException {
		xmlWriter.writeStr(name, toExternal(f));

	}

	@Override
	public void write(TextResponseWriter writer, String name, Fieldable f)
			throws IOException {
		writer.writeStr(name, toExternal(f), false);

	}

	@Override
	public String toInternal(String val) {
		return DistanceUtils.lngToString(new Double(val).doubleValue());
	}

	@Override
	public String toExternal(Fieldable f) {
		return new Double(DistanceUtils.stringToLng(f.stringValue()))
				.toString();
	}
}
