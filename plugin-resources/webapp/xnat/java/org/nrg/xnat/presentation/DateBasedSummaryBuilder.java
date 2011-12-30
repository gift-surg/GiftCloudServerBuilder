package org.nrg.xnat.presentation;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nrg.xft.presentation.FlattenedItemI;


public class DateBasedSummaryBuilder extends ChangeSummaryBuilderA {

	public DateBasedSummaryBuilder(EventBuilderI b) {
		super(b);
	}

	public static Map<Date,ChangeSummary> build(List<FlattenedItemI> items,EventBuilderI b) throws Exception{
		return (new DateBasedSummaryBuilder(b)).call(items);
	}
	
}