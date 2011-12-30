package org.nrg.xnat.turbine.modules.screens;

import java.util.Arrays;
import java.util.List;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xft.presentation.FlattenedItemA;
import org.nrg.xft.presentation.FlattenedItemI;
import org.nrg.xft.presentation.ItemMerger;
import org.nrg.xft.presentation.ItemPropBuilder;
import org.nrg.xnat.itemBuilders.FullFileHistoryBuilder;
import org.nrg.xnat.presentation.DateBasedSummaryBuilder;

public class HistoryNavigator extends SecureReport {

	@Override
	public void finalProcessing(RunData data, Context context) {
		try {

			List<FlattenedItemI> items=
				Arrays.asList(
					ItemMerger.merge(
							ItemPropBuilder.build(item.getItem(), FlattenedItemA.GET_ALL,Arrays.asList(new FullFileHistoryBuilder()))));
			
			context.put("change_sets",(new DateBasedSummaryBuilder(null)).call(items));
		} catch (Exception e) {
			logger.error("",e);
		}
		
	}

}
