package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;

public class DicomScanTable extends SecureScreen {

	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
		XFTTable t=null;
		if(TurbineUtils.HasPassedParameter("table_tag", data)){
			String tag=(String)TurbineUtils.GetPassedParameter("table_tag", data);
			t=(XFTTable)data.getSession().getAttribute(tag);
			context.put("table", t);
			data.getSession().removeAttribute(tag);
		}
	}

}
