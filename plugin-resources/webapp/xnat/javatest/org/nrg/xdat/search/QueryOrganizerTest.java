/*
 * org.nrg.xdat.search.QueryOrganizerTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xdat.search;

import org.junit.Test;
import org.nrg.test.BaseXDATTestCase;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperField;
import org.nrg.xft.search.SearchCriteria;

public class QueryOrganizerTest extends BaseXDATTestCase {

	@Test
	public void testHistoryQueryOnExtended() throws Exception {
		GenericWrapperElement primary_input = GenericWrapperElement
				.GetElement("xnat:abstractResource_history");
		GenericWrapperElement surrogate_input = GenericWrapperElement
				.GetElement("xnat:abstractResource");

		final Object[][] surrogateKeyArray = surrogate_input.getSQLKeys();

		QueryOrganizer qo = new QueryOrganizer(primary_input, null,
				ViewManager.ALL);
		qo.addField(primary_input.getFullXMLName()
				+ "/extension_item/element_name");
		qo.addField(primary_input.getFullXMLName() + "/xft_version");
		qo.addField(primary_input.getFullXMLName() + "/history_id");

		CriteriaCollection cc = new CriteriaCollection("AND");

		SearchCriteria sc = new SearchCriteria();
		sc.setFieldWXMLPath(primary_input.getFullXMLName() + "/history_id");
		sc.setOverrideFormatting(true);
		sc.setComparison_type(" IS NOT ");
		sc.setValue("NULL");
		cc.add(sc);
		for (int i = 0; i < surrogateKeyArray.length; i++) {
			GenericWrapperField gwf = (GenericWrapperField) surrogateKeyArray[i][3];
			qo.addField(gwf.getXMLPathString(primary_input.getFullXMLName()));
		}

		String query = "SELECT * FROM (" + qo.buildQuery() + ") SEARCH  WHERE "
				+ cc.getSQLClause(qo);
		System.out.println(query);

		XFTTable.Execute(query, PoolDBUtils.getDefaultDBName(), "");
	}
}
