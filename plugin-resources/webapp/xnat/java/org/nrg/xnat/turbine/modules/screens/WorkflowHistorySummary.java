package org.nrg.xnat.turbine.modules.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.DateUtils;
import org.nrg.xnat.itemBuilders.WorkflowBasedHistoryBuilder;
import org.nrg.xnat.itemBuilders.WorkflowBasedHistoryBuilder.WorkflowView;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA;


public class WorkflowHistorySummary extends SecureReport {

	@Override
	public void finalProcessing(RunData data, Context context) {
		try {
			String id=item.getStringProperty("ID");
			List<WorkflowView> wvs=new ArrayList<WorkflowView>();
			
			Object o=TurbineUtils.GetPassedParameter("includeFiles", data);
			Object details=TurbineUtils.GetPassedParameter("includeDetails", data);
			final boolean includeFiles=(o==null || Boolean.valueOf(o.toString()));
			final boolean includedetails=(details==null || Boolean.valueOf(details.toString()));
			
			Map<Number,WorkflowView> map=(new WorkflowBasedHistoryBuilder(item,id,TurbineUtils.getUser(data),includeFiles,includedetails)).call();
			wvs.addAll(map.values());
			
			Collections.sort(wvs, new Comparator<WorkflowView>() {
				@Override
				public int compare(WorkflowView arg0, WorkflowView arg1) {
					try {
						return DateUtils.compare(arg0.getDate(), arg1.getDate());
					} catch (Exception e) {
						return -1;
					}
				}
			});
			context.put("change_sets",wvs);
		} catch (Exception e) {
			logger.error("",e);
		}
		
	}
}