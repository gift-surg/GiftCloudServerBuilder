// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Mon Dec 08 15:25:31 CST 2008
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nrg.pipeline.PipelineRepositoryManager;
import org.nrg.xdat.model.ArcProjectDescendantI;
import org.nrg.xdat.model.ArcProjectDescendantPipelineI;
import org.nrg.xdat.model.ArcProjectPipelineI;
import org.nrg.xdat.model.PipePipelinedetailsElementI;
import org.nrg.xdat.model.PipePipelinedetailsI;
import org.nrg.xdat.model.PipePipelinedetailsParameterI;
import org.nrg.xdat.model.XnatAbstractprotocolI;
import org.nrg.xdat.om.ArcPipelinedata;
import org.nrg.xdat.om.ArcPipelineparameterdata;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectDescendant;
import org.nrg.xdat.om.ArcProjectDescendantPipeline;
import org.nrg.xdat.om.ArcProjectPipeline;
import org.nrg.xdat.om.PipePipelinedetails;
import org.nrg.xdat.om.PipePipelinedetailsElement;
import org.nrg.xdat.om.PipePipelinedetailsParameter;
import org.nrg.xdat.om.XnatAbstractprotocol;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.auto.AutoPipePipelinerepository;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.ajax.writer.ResponseWriterI;
import org.nrg.xnat.ajax.writer.WriterFactory;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BasePipePipelinerepository extends AutoPipePipelinerepository {

	public BasePipePipelinerepository(ItemI item)
	{
		super(item);
	}

	public BasePipePipelinerepository(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BasePipePipelinerepository(UserI user)
	 **/
	public BasePipePipelinerepository()
	{}

	public BasePipePipelinerepository(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
	public void write(HttpServletRequest request, HttpServletResponse response) throws IOException {
		XFTTable resultsTable = getPipelines();
		write(resultsTable, request, response);
	}

	public void write(String projectId, String dataType, HttpServletRequest request, HttpServletResponse response) throws IOException {
		String additional = request.getParameter("additional");
		boolean additionalVal = false;
		if (additional != null) {
			additionalVal = Boolean.parseBoolean(additional);
		}
		XFTTable resultsTable = getResultsTable(projectId, dataType, additionalVal);
		write(resultsTable, request, response);
	}


	private void write(XFTTable resultsTable, HttpServletRequest request, HttpServletResponse response) throws IOException  {
		ResponseWriterI writer = WriterFactory.getWriter(request, response);
		String[] cols = resultsTable.getColumns();
		String siteOrProject = cols[cols.length - 1];
		Hashtable<String,ArrayList<String>> metaFields = new Hashtable<String, ArrayList<String>>();
		ArrayList<String> sourceValues = new ArrayList<String>();
		sourceValues.add(siteOrProject);
		metaFields.put("Source", sourceValues);
		writer.setMetaFields(metaFields);
		XFTTable dataTable = reFormatTable(resultsTable);
		writer.write(dataTable, "All Pipelines");
	}

	private XFTTable reFormatTable(XFTTable results) {
		XFTTable dataTable = new XFTTable();
		String[] cols = results.getColumns();
		int lastCol = cols.length - 1 ;
		if (cols[lastCol].equalsIgnoreCase("SITE") || cols[lastCol].equalsIgnoreCase("PROJECT")) {
			String[] newCols = new String[cols.length - 1];
			for (int i = 0 ; i < cols.length -1 ; i++) {
				newCols[i] = cols[i];
			}
			dataTable.initTable(newCols);
			results.resetRowCursor();
			while (results.hasMoreRows()) {
				Object[] currentRow = results.nextRow();
				Object[] rowEntries = new Object[newCols.length];
				for (int i = 0; i < newCols.length; i++) {
					rowEntries[i] = currentRow[i];
				}
				dataTable.insertRow(rowEntries);
			}
		}else {
			dataTable =  results;
		}
		return dataTable;
	}


	public XFTTable toTable(ArcProject arcProject) {
		XFTTable table = new XFTTable();
		ArrayList<String> cols = new ArrayList<String>();
		cols.add("Applies To");
		cols.add("Generates"); cols.add("Name"); cols.add("Description"); cols.add("Path");
		cols.add("Datatype");
		table.initTable(cols);
		List<ArcProjectPipelineI> pipelines = arcProject.getPipelines_pipeline();
		if (pipelines.size() > 0 ) {
			for (int i = 0; i < pipelines.size(); i++) {
				PipePipelinedetails pipeline = getPipeline(pipelines.get(i).getLocation());
				String[] rowEntries = new String[6];
				rowEntries[0] = getDisplayName(XnatProjectdata.SCHEMA_ELEMENT_NAME);
				rowEntries[1] = getElementsGeneratedBy(pipeline);
				rowEntries[2] = pipelines.get(i).getName();
				rowEntries[3] = pipeline.getDescription();
				rowEntries[4] = pipelines.get(i).getLocation();
				rowEntries[5] = pipeline.getAppliesto();
				table.insertRow(rowEntries);
			}
		}
		List<ArcProjectDescendantI> descendants = arcProject.getPipelines_descendants_descendant();
		if (descendants.size() > 0 ) {
			for (int i = 0; i < descendants.size(); i++) {
				List<ArcProjectDescendantPipelineI> pipelinesDesc = descendants.get(i).getPipeline();
				for (int j = 0; j < pipelinesDesc.size(); j++) {
					PipePipelinedetails pipeline = getPipeline(pipelinesDesc.get(j).getLocation());
					String[] rowEntries = new String[6];
					rowEntries[0] = getDisplayName(descendants.get(i).getXsitype());
					rowEntries[1] = getElementsGeneratedBy(pipeline);
					rowEntries[2] = pipelinesDesc.get(j).getName();
					rowEntries[3] = pipeline.getDescription();
					rowEntries[4] = pipelinesDesc.get(j).getLocation();
					rowEntries[5] = pipeline.getAppliesto();
					table.insertRow(rowEntries);
				}
			}
		}
		return table;
	}

	public PipePipelinedetails getPipeline(String path) {
		PipePipelinedetails rtn = null;
		List<PipePipelinedetailsI> pipelines = getPipeline();
		for (int i = 0; i < pipelines.size(); i++) {
			PipePipelinedetails pipeline = (PipePipelinedetails)pipelines.get(i);
			if (pipeline.getPath().equals(path)) {
				rtn = pipeline;
				break;
			}
		}
		return rtn;
	}

	private XFTTable getResultsTable(XnatProjectdata project, boolean additionalVal) {
		if (project == null) return new XFTTable();
		ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(project.getId());
		if (arcProject == null) { // No Project set in the archive specification
			return getFilteredResultsTable(project);
		}
		XFTTable rtn = new XFTTable();
		ArrayList<String> cols = new ArrayList<String>();
		cols.add("DataType");
		cols.add("Generates"); cols.add("Description"); cols.add("Project");
		rtn.initTable(cols);
		List<ArcProjectPipelineI> pipelines = arcProject.getPipelines_pipeline();
		if (pipelines.size() == 0) { // No pipelines have been set for the project. Return site wide project specific ones
			return getFilteredResultsTable(project);
		}
		if (additionalVal) { //Return the pipelines which are available on the site but not selected for the project
			return getAdditionalResultsTable(project);
		}
		for (int i = 0; i < pipelines.size(); i++) {
			String[] rowEntries = new String[4];
			rowEntries[0] = getDisplayName(project.SCHEMA_ELEMENT_NAME);
			rowEntries[1] = lookupElementsGeneratedBy(project.SCHEMA_ELEMENT_NAME, pipelines.get(i).getLocation() + File.separator + pipelines.get(i).getName());
			rowEntries[2] = pipelines.get(i).getDescription();
			rowEntries[3] = "Project";
			rtn.insertRow(rowEntries);
		}
		//Get all the pipelines for the associated datatypes
		List<XnatAbstractprotocolI> studyProtocols = project.getStudyprotocol();
		if (studyProtocols.size() == 0) return rtn;
		for (int i = 0; i < studyProtocols.size(); i++) {
			XnatAbstractprotocol protocol = (XnatAbstractprotocol)studyProtocols.get(i);
			XFTTable results = getResultsTable(project.getId(), protocol.getDataType());
			if (results.getNumRows() >= 1) {
				while (results.hasMoreRows()) {
					String[] newRow = new String[4];
					Object[] resultsRow = results.nextRow();
					newRow[0] = getDisplayName(protocol.getDataType());
					for (int j = 1; j < 4; j++)
					 newRow[j] = (String)resultsRow[j-1];
					rtn.insertRow(newRow);
				}
			}
		}
		return rtn;
	}


	private Hashtable<String, ArrayList<PipePipelinedetails>> getPipelinesPerDataType() {
		Hashtable<String, ArrayList<PipePipelinedetails>> rtn = new Hashtable<String, ArrayList<PipePipelinedetails>>();
		List<PipePipelinedetailsI> pipelines = getPipeline();
		if (pipelines.size() > 0 ) {
			for (int i = 0; i < pipelines.size(); i++) {
				PipePipelinedetails pipeline = (PipePipelinedetails)pipelines.get(i);
				if (rtn.containsKey(pipeline.getAppliesto())) {
					ArrayList<PipePipelinedetails> groupedByAppliesTo = rtn.get(pipeline.getAppliesto());
					groupedByAppliesTo.add(pipeline);
					rtn.put(pipeline.getAppliesto(),groupedByAppliesTo );
				}else {
					ArrayList<PipePipelinedetails> groupedByAppliesTo = new ArrayList<PipePipelinedetails>();
					groupedByAppliesTo.add(pipeline);
					rtn.put(pipeline.getAppliesto(), groupedByAppliesTo);
				}
			}
		}
		return rtn;
	}


	//Returns all the pipelines in repository with the Site
	public XFTTable getPipelines() {
		XFTTable rtn = new XFTTable();
		ArrayList<String> cols = new ArrayList<String>();
		cols.add("DataType");
		cols.add("Path"); cols.add("Description");cols.add("Site");
		rtn.initTable(cols);
		Hashtable<String, ArrayList<PipePipelinedetails>> repository = getPipelinesPerDataType();
	    Enumeration<String> enume = repository.keys();
	    while (enume.hasMoreElements()) {
	    	String dataType = enume.nextElement();
	    	ArrayList<PipePipelinedetails> pipelines = repository.get(dataType);
	    	for (int i = 0; i < pipelines.size(); i++) {
	    		PipePipelinedetails pipeline = pipelines.get(i);
		    	String[] row = new String[4];
		    	row[0] = dataType;
		    	row[1] = pipeline.getPath();
		    	row[2] = pipeline.getDescription();
		    	row[3] = "Site";
		    	rtn.insertRow(row);
	    	}
	    }
	    return rtn;
	}


	private XFTTable getResultsTable(String projectId, String descendant) {
		return getResultsTable(projectId, descendant, false);
	}

	private XFTTable getResultsTable(String projectId, String descendant, boolean additional) {
		ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(projectId);
		if (arcProject == null) return getResultsTable();
		XFTTable rtn = new XFTTable();
		ArrayList<String> cols = new ArrayList<String>();
		cols.add("Generates"); cols.add("Description"); cols.add("Project");
		rtn.initTable(cols);
		ArcProjectDescendant descendantEle = arcProject.getDescendant(descendant);
		if (descendantEle != null) {
			List<ArcProjectDescendantPipelineI> pipelines = descendantEle.getPipeline();
			for (int i = 0; i < pipelines.size(); i++) {
				String[] rowEntries = new String[3];
				rowEntries[0] = lookupElementsGeneratedBy(descendant, pipelines.get(i).getLocation() + File.separator + pipelines.get(i).getName());
				rowEntries[1] = pipelines.get(i).getDescription();
				rowEntries[2] = "Project";
				rtn.insertRow(rowEntries);
			}
		}else {
			//No Pipelines have been set for this studyProtocol in the project
			rtn = new XFTTable();
			cols = new ArrayList<String>();
			cols.add("Generates"); cols.add("Description"); cols.add("Site");
			rtn.initTable(cols);
			ArrayList<PipePipelinedetails> pipelines = getAllPipelines(descendant);
			if (pipelines == null) return new XFTTable();
			for (int i = 0; i < pipelines.size(); i++) {
				String[] rowEntries = new String[3];
				rowEntries[0] = getElementsGeneratedBy(pipelines.get(i));
				rowEntries[1] = pipelines.get(i).getDescription();
				rowEntries[2] = "Site";
				rtn.insertRow(rowEntries);
			}
		}
		return rtn;
	}





	private  Hashtable<String, ArrayList<PipePipelinedetails>>  getAllPipelines() {
		return getPipelinesPerDataType();
	}

	public  ArrayList<PipePipelinedetails> getAllPipelines(String dataType) {
		Hashtable<String, ArrayList<PipePipelinedetails>> repository = getPipelinesPerDataType();
		return repository.get(dataType);
	}

	public  Enumeration<String> getAllDataTypesWithAvailablePipelines() {
		Hashtable<String, ArrayList<PipePipelinedetails>> repository = getPipelinesPerDataType();
		return repository.keys();
	}

	public  ArrayList<String> getAllDataTypesWithAvailablePipelinesAsCollection() {
		ArrayList<String> rtn = new ArrayList<String>();
		Hashtable<String, ArrayList<PipePipelinedetails>> repository = getPipelinesPerDataType();
		Enumeration<String> enume = repository.keys();
		while (enume.hasMoreElements()) {
			rtn.add(enume.nextElement());
		}
		return rtn;
	}

	public Hashtable<String, ArrayList<ArcPipelinedata>> getPipelinesForProject(XnatProjectdata project) throws Exception {
		Hashtable<String, ArrayList<ArcPipelinedata>> rtn = new Hashtable<String,ArrayList<ArcPipelinedata>>();
		if (project == null) return rtn;
		/*ArrayList<XnatAbstractprotocol> studyProtocols = new ArrayList<XnatAbstractprotocol>();
		studyProtocols.addAll(project.getStudyprotocol());
		XnatAbstractprotocol projectDataType = new XnatAbstractprotocol();
		projectDataType.setDataType("xnat:projectData");
		studyProtocols.add(projectDataType);
		for (int i = 0; i < studyProtocols.size(); i++) {
			XnatAbstractprotocol protocol = studyProtocols.get(i);
			ArrayList<ArcPipelinedata> pipelines = getPipelinesForDatatype(protocol.getDataType());
			if (pipelines.size() > 0) {
				ArrayList<ArcPipelinedata> values = pipelines;
				if (rtn.containsKey(protocol.getDataType())) {
					values = rtn.get(protocol.getDataType());
					values.addAll(pipelines);
				}
				rtn.put(protocol.getDataType(), values);
			}
		}*/
		Hashtable<String, ArrayList<PipePipelinedetails>> repository = getPipelinesPerDataType();
		if (repository == null) return rtn;
		Enumeration<String> keys = repository.keys();
		while (keys.hasMoreElements()) {
			String xsiType = keys.nextElement();
			ArrayList pipelinesForDataType = new ArrayList();
			if (xsiType.equalsIgnoreCase(XnatProjectdata.SCHEMA_ELEMENT_NAME))
				pipelinesForDataType = getArcPipelinesForProject(repository.get(xsiType));
			else
				pipelinesForDataType = getArcPipelinesForDescendant(repository.get(xsiType));
			if (pipelinesForDataType.size() > 0) {
				ArrayList<ArcPipelinedata> values = pipelinesForDataType;
				if (rtn.containsKey(xsiType)) {
					values = rtn.get(xsiType);
					values.addAll(pipelinesForDataType);
				}
				rtn.put(xsiType, values);
			}
		}

		return rtn;
	}

	private ArrayList getPipelinesForDatatype(String xsiType) throws Exception{
		ArrayList rtn = new ArrayList();
		Hashtable<String, ArrayList<PipePipelinedetails>> repository = getPipelinesPerDataType();
		if (repository == null) return rtn;
		if (repository.containsKey(xsiType)) {
			if (xsiType.equalsIgnoreCase(XnatProjectdata.SCHEMA_ELEMENT_NAME))
				rtn = getArcPipelinesForProject(repository.get(xsiType));
			else
				rtn = getArcPipelinesForDescendant(repository.get(xsiType));

		}
		return rtn;
	}

	public ArrayList<ArcProjectPipeline> getArcPipelinesForProject(ArrayList<PipePipelinedetails> pipelines) throws Exception {
		ArrayList<ArcProjectPipeline> rtn = new ArrayList<ArcProjectPipeline>();
		if (pipelines == null || pipelines.size() < 1) {
			return rtn;
		}
		for (int i = 0; i < pipelines.size(); i++) {
			PipePipelinedetails pipeline = pipelines.get(i);
			ArcProjectPipeline pBean = new ArcProjectPipeline();
			String description = pipeline.getDescription();
			if (description != null)
				description += " XNAT DATATYPE: " +pipeline.getAppliesto()  + " Applies To: " + getDisplayName(pipeline.getAppliesto()) + " Generates: " + getElementsGeneratedBy(pipeline);
			else
				description =  " XNAT DATATYPE: " +pipeline.getAppliesto()  +" Applies To: " + getDisplayName(pipeline.getAppliesto()) + " Generates: " + getElementsGeneratedBy(pipeline);
			pBean.setDescription(description);
			String path = pipeline.getPath();
			File pipelineDescriptor = new File(path);
			//if (pipelineDescriptor.exists()) {
				pBean.setName(pipelineDescriptor.getName());
				pBean.setLocation(path);
				pBean.setDisplaytext(pipelineDescriptor.getName());
			//}
			String customwebpage = pipeline.getCustomwebpage();
			if (customwebpage != null)
				pBean.setCustomwebpage(customwebpage);
			List<PipePipelinedetailsParameterI> parameters = pipeline.getParameters_parameter();
			if (parameters != null && parameters.size() > 0 ) {
				for (int j = 0; j < parameters.size(); j++) {
					PipePipelinedetailsParameterI parameter = parameters.get(j);
					ArcPipelineparameterdata paramData = new ArcPipelineparameterdata();
					paramData.setName(parameter.getName());
					description = parameter.getDescription();
					if (description != null) {
						paramData.setDescription(description);
					}
					String value = parameter.getValues_schemalink();
					if (value != null)
						paramData.setSchemalink(value);
					else {
						value = parameter.getValues_csvvalues();
						paramData.setCsvvalues(value);
					}
					pBean.setParameters_parameter(paramData);
			  }
			}
			rtn.add(pBean);
		}
		return rtn;
	}

	public boolean delete(ArcProject arcProject1, String pathToPipeline, String dataType, XDATUser user)  throws Exception {
		boolean success = true;
		ArcProject arcProject = new ArcProject();
		arcProject.setItem(arcProject1.getCurrentDBVersion(true));
		boolean save = false;
		try {
		if (dataType.equalsIgnoreCase(XnatProjectdata.SCHEMA_ELEMENT_NAME)) {
			List<ArcProjectPipelineI> pipelines = arcProject.getPipelines_pipeline();
			for (int i = 0; i < pipelines.size(); i++) {
				ArcProjectPipeline pipeline = (ArcProjectPipeline)pipelines.get(i);
				if (pipeline.getLocation().equals(pathToPipeline)) {
					arcProject.removePipelines_pipeline(i);
					save = true;
				}
			}
		}else {
			ArcProjectDescendant descendant = arcProject.getDescendant(dataType);
			if (descendant == null) success = false;
			else {
				List<ArcProjectDescendantPipelineI> pipelines =  descendant.getPipeline();
				for (int i = 0; i < pipelines.size(); i++) {
					ArcProjectDescendantPipeline pipeline = (ArcProjectDescendantPipeline)pipelines.get(i);
					if (pipeline.getLocation().equals(pathToPipeline)) {
						descendant.removePipeline(i);
						save = true;
					}
				}
			}
		}
		if (save) arcProject.save(user, false, true);
		}catch(Exception e) {
			success = false;
			throw e;
		}
		return success;
	}


	public ArrayList<ArcProjectDescendantPipeline> getArcPipelinesForDescendant(ArrayList<PipePipelinedetails> pipelines) throws Exception  {
		ArrayList<ArcProjectDescendantPipeline> rtn = new ArrayList<ArcProjectDescendantPipeline>();
		if (pipelines == null || pipelines.size() < 1) {
			return rtn;
		}
		for (int i = 0; i < pipelines.size(); i++) {
			PipePipelinedetails pipeline = pipelines.get(i);
			ArcProjectDescendantPipeline pBean = new ArcProjectDescendantPipeline();
			String description = pipeline.getDescription();
			if (description != null)
				description += " XNAT DATATYPE: " +pipeline.getAppliesto()  + " Applies To: " + getDisplayName(pipeline.getAppliesto()) + " Generates: " + getElementsGeneratedBy(pipeline);
			else
				description =  " XNAT DATATYPE: " +pipeline.getAppliesto()  + " Applies To: " + getDisplayName(pipeline.getAppliesto()) + " Generates: " + getElementsGeneratedBy(pipeline);
			pBean.setDescription(description);
			String path = pipeline.getPath();
			File pipelineDescriptor = new File(path);
			//if (pipelineDescriptor.exists()) {
				pBean.setName(pipelineDescriptor.getName());
				pBean.setLocation(path);
				pBean.setDisplaytext(pipelineDescriptor.getName());
			//}
			String customwebpage = pipeline.getCustomwebpage();
			if (customwebpage != null)
				pBean.setCustomwebpage(customwebpage);
			List<PipePipelinedetailsParameterI> parameters = pipeline.getParameters_parameter();
			if (parameters != null && parameters.size() > 0 ) {
				for (int j = 0; j < parameters.size(); j++) {
					PipePipelinedetailsParameter parameter = (PipePipelinedetailsParameter)parameters.get(j);
					ArcPipelineparameterdata paramData = new ArcPipelineparameterdata();
					paramData.setName(parameter.getName());
					description = parameter.getDescription();
					if (description != null) {
						paramData.setDescription(description);
					}
					String value = parameter.getValues_schemalink();
					if (value != null)
						paramData.setSchemalink(value);
					else {
						value = parameter.getValues_csvvalues();
						paramData.setCsvvalues(value);
					}
					pBean.setParameters_parameter(paramData);
				}
			}
			rtn.add(pBean);
		}
		return rtn;
	}

	public ArcPipelineparameterdata convertToArcPipelineParameter(PipePipelinedetailsParameter parameter) {
		ArcPipelineparameterdata paramData = new ArcPipelineparameterdata();
		paramData.setName(parameter.getName());
		String description = parameter.getDescription();
		if (description != null) {
			paramData.setDescription(description);
		}
		String value = parameter.getValues_schemalink();
		if (value != null)
			paramData.setSchemalink(value);
		else {
			value = parameter.getValues_csvvalues();
			paramData.setCsvvalues(value);
		}
		return paramData;
	}

	public Hashtable<String, String> getGeneratedElementsByAllPipelines() {
		Hashtable<String, String> rtn = new Hashtable<String, String>();
		Hashtable<String, ArrayList<PipePipelinedetails>> repository = getPipelinesPerDataType();
		Enumeration<String> enume = repository.keys();
		while (enume.hasMoreElements()) {
			String dataType = enume.nextElement();
			ArrayList<PipePipelinedetails> pipelinesForDataType = repository.get(dataType);
			for (int i = 0; i < pipelinesForDataType.size(); i++) {
				PipePipelinedetails pipeline = pipelinesForDataType.get(i);
				List<PipePipelinedetailsElementI> generatedElements = pipeline.getGenerateselements_element();
				if (generatedElements.size() > 0) {
					for (int j = 0; j < generatedElements.size(); j++) {
						rtn.put(generatedElements.get(j).getElement().trim(), "");
					}
				}
			}
		}
		return rtn;
	}

	public void write(XnatProjectdata project, HttpServletRequest request, HttpServletResponse response) throws IOException {
		String additional = request.getParameter("additional");
		boolean additionalVal = false;
		if (additional != null) {
			additionalVal = Boolean.parseBoolean(additional);
		}
		XFTTable resultsTable = getResultsTable(project, additionalVal);
		write(resultsTable, request, response);
	}

	private boolean exists(List<ArcProjectPipelineI> pipelines, String path) {
		boolean exists = false;
		if (pipelines == null || path == null || pipelines.size() == 0) return false;
		for (int i = 0; i < pipelines.size(); i++) {
			String pipelinePath = pipelines.get(i).getLocation(); // + File.separator + pipelines.get(i).getName();
			if (pipelinePath.equals(path)) {
				exists = true;
				break;
			}
		}
		return exists;
	}

	private boolean existsDesc(List<ArcProjectDescendantPipelineI> pipelines, String path) {
		boolean exists = false;
		if (pipelines == null || path == null || pipelines.size() == 0 ) return exists;
		for (int i = 0; i < pipelines.size(); i++) {
			String pipelinePath = pipelines.get(i).getLocation(); // + File.separator + pipelines.get(i).getName();
			if (pipelinePath.equals(path)) {
				exists = true;
				break;
			}
		}
		return exists;
	}



	public String getElementsGeneratedBy(PipePipelinedetails pipeline) {
		String rtn = "";
		List<PipePipelinedetailsElementI> generatedElements = pipeline.getGenerateselements_element();
		if (generatedElements.size() > 0) {
				for (int i = 0; i < generatedElements.size(); i++) {
					rtn +=getDisplayName(generatedElements.get(i).getElement()) + ",";
				}
		}
		if (rtn.endsWith(",")) rtn = rtn.substring(0, rtn.length() - 1);
		return rtn;
	}

	public  String getDisplayName(String dataType) {
		 String rtn = dataType;
		 try {
			 Hashtable<String,ElementSecurity> ess = ElementSecurity.GetElementSecurities();
			 rtn =  ess.get(dataType).getPluralDescription();
		 }catch(Exception e) {}
		 return rtn;
	}


	private String lookupElementsGeneratedBy(String dataType, String path) {
		String rtn = "";
		Hashtable<String, ArrayList<PipePipelinedetails>> repository = getPipelinesPerDataType();
	    ArrayList<PipePipelinedetails> pipelines =  repository.get(dataType);
	    for (int i = 0; i < pipelines.size(); i++) {
	    	PipePipelinedetails pipeline = pipelines.get(i);
	    	String pipelinePath = pipeline.getPath();
	    	if (pipelinePath.equals(path)) {
	    		rtn = getElementsGeneratedBy(pipeline);
	    		break;
	    	}
	    }
		return rtn;
	}

	private XFTTable getFilteredResultsTable(XnatProjectdata project) {
		if (project == null) return new XFTTable();
		XFTTable rtn = new XFTTable();
		ArrayList<String> cols = new ArrayList<String>();
		cols.add("DataType");
		cols.add("Generates"); cols.add("Description"); cols.add("Site");
		rtn.initTable(cols);
		ArrayList<PipePipelinedetails> pipelines = getAllPipelines(project.SCHEMA_ELEMENT_NAME);
		if (pipelines != null && pipelines.size() > 0) {
			for (int i = 0; i < pipelines.size(); i++) {
				String[] rowEntries = new String[4];
				rowEntries[0] = getDisplayName(project.SCHEMA_ELEMENT_NAME);
				rowEntries[1] = getElementsGeneratedBy(pipelines.get(i));
				rowEntries[2] = pipelines.get(i).getDescription();
				rowEntries[3] = "Site";
				rtn.insertRow(rowEntries);
			}
		}
		List<XnatAbstractprotocolI> studyProtocols = project.getStudyprotocol();
		if (studyProtocols.size() == 0) return rtn;
		for (int i = 0; i < studyProtocols.size(); i++) {
			XnatAbstractprotocol protocol = (XnatAbstractprotocol)studyProtocols.get(i);
			XFTTable results = getResultsTable(project.getId(), protocol.getDataType());
			if (results.getNumRows() >= 1) {
				while (results.hasMoreRows()) {
					String[] newRow = new String[4];
					Object[] resultsRow = results.nextRow();
					newRow[0] = getDisplayName(protocol.getDataType());
					for (int j = 1; j < 4; j++)
					 newRow[j] = (String)resultsRow[j-1];
					rtn.insertRow(newRow);
				}
			}
		}
		return rtn;
	}

	private XFTTable getAdditionalResultsTable(XnatProjectdata project) {
		if (project == null) return new XFTTable();
		XFTTable rtn = new XFTTable();
		ArrayList<String> cols = new ArrayList<String>();
		cols.add("DataType");
		cols.add("Generates"); cols.add("Description"); cols.add("Site");
		rtn.initTable(cols);
		ArrayList<PipePipelinedetails> pipelines = getAllPipelines(project.SCHEMA_ELEMENT_NAME);
		if (pipelines == null || pipelines.size() == 0) {
			return new XFTTable();
		}
		ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(project.getId());
		List<ArcProjectPipelineI> projectSelectedPipelines = arcProject.getPipelines_pipeline();
		//Gather only those pipelines which have not been selected.
		ArrayList<PipePipelinedetails> additionalPipelines = new ArrayList<PipePipelinedetails>();
		for (int i = 0; i < pipelines.size(); i++) {
			String id = pipelines.get(i).getPath();
			if (!exists(projectSelectedPipelines, id)) {
				additionalPipelines.add(pipelines.get(i));
			}
		}
		for (int i = 0; i < additionalPipelines.size(); i++) {
			String[] rowEntries = new String[4];
			rowEntries[0] = getDisplayName(project.SCHEMA_ELEMENT_NAME);
			rowEntries[1] = getElementsGeneratedBy(additionalPipelines.get(i));
			rowEntries[2] = additionalPipelines.get(i).getDescription();
			rowEntries[3] = "Site";
			rtn.insertRow(rowEntries);
		}
		List<XnatAbstractprotocolI> studyProtocols = project.getStudyprotocol();
		if (studyProtocols.size() == 0) return rtn;
		Hashtable<String, ArrayList<PipePipelinedetails>> additionalPipelineHash = new Hashtable<String,ArrayList<PipePipelinedetails>>();
		for (int i = 0; i < studyProtocols.size(); i++) {
			XnatAbstractprotocol protocol = (XnatAbstractprotocol)studyProtocols.get(i);
			ArrayList<PipePipelinedetails> descpipelines =  getAllPipelines(protocol.getDataType());
			List<ArcProjectDescendantPipelineI> projectSelectedDescPipelines = arcProject.getPipelinesForDescendant(protocol.getDataType());
			ArrayList<PipePipelinedetails> additionalDescPipelines = new ArrayList<PipePipelinedetails>();
			if (descpipelines != null) {
				for (int j = 0; j < descpipelines.size() ; j++) {
					String id = descpipelines.get(j).getPath();
					if (!existsDesc(projectSelectedDescPipelines, id)) {
						additionalDescPipelines.add(descpipelines.get(i));
					}
				}
				additionalPipelineHash.put(protocol.getDataType(), additionalDescPipelines);
			}
		}
		Enumeration<String> dataTypes = additionalPipelineHash.keys();
		while (dataTypes.hasMoreElements()) {
			String dataType = dataTypes.nextElement();
			ArrayList<PipePipelinedetails> additionalDescPipelines = additionalPipelineHash.get(dataType);
			for (int i = 0; i < additionalDescPipelines.size(); i++) {
				String[] rowEntries = new String[4];
				rowEntries[0] = getDisplayName(dataType);
				rowEntries[1] = getElementsGeneratedBy(additionalDescPipelines.get(i));
				rowEntries[2] = additionalDescPipelines.get(i).getDescription();
				rowEntries[3] = "Site";
				rtn.insertRow(rowEntries);
			}
		}
		return rtn;
	}


	public ArcProject createNewArcProject(XnatProjectdata proj) throws Exception {
		ArcProject arcProject = new ArcProject();
		if (proj == null) return arcProject;
		Hashtable<String, ArrayList<ArcPipelinedata>>  pipelinesHash = PipelineRepositoryManager.GetInstance().getPipelinesForProject(proj);
			if (pipelinesHash != null && pipelinesHash.size() > 0) {
				Enumeration<String> keys = pipelinesHash.keys();
				while (keys.hasMoreElements()) {
					String dataType = keys.nextElement();
					if (dataType.equals(proj.SCHEMA_ELEMENT_NAME)) {
						ArrayList<ArcPipelinedata> projectPipelines = pipelinesHash.get(proj.SCHEMA_ELEMENT_NAME);
						if (projectPipelines != null && projectPipelines.size() > 0) {
							for (int i = 0; i < projectPipelines.size(); i++) {
								arcProject.setPipelines_pipeline(projectPipelines.get(i));
							}
						}
					}else {
						ArrayList<ArcPipelinedata> projectPipelines = pipelinesHash.get(dataType);
						if (projectPipelines != null && projectPipelines.size() > 0) {
							ArcProjectDescendant arcProjectDesc = new ArcProjectDescendant();
							arcProjectDesc.setXsitype(dataType);
							for (int i = 0; i < projectPipelines.size(); i++) {
								arcProjectDesc.setPipeline(projectPipelines.get(i));
							}
							arcProject.setPipelines_descendants_descendant(arcProjectDesc);
						}
					}
				}
			}
			return arcProject;
	}

	public ArcProject getAdditionalPipelines(XnatProjectdata project) throws Exception{
		ArcProject arcProject = new ArcProject();
		ArcProject rtn = new ArcProject();
		if (project == null) return new ArcProject();
		Hashtable<String, ArrayList<ArcPipelinedata>>  pipelinesHash = getPipelinesForProject(project);
		ArcProject arcProjectFromSpec = ArcSpecManager.GetInstance().getProjectArc(project.getId());
		if (arcProjectFromSpec == null) return createNewArcProject(project);
		//TO DO
		//Hack for now
		arcProject.setItem((XFTItem)arcProjectFromSpec.getCurrentDBVersion(true).clone());
		rtn.setItem((XFTItem)arcProjectFromSpec.getCurrentDBVersion(true).clone());
		//Clear all the existing pipelines as we want only the additional pipelines that are applicable for this project
		rtn.removeAllPipelines();

		//TO DO
		//DIRTY part here
/*		try {
			ItemCollection pipelinesNode = null;
			try {
				pipelinesNode = arcProject.getChildItemCollection(arcProject.SCHEMA_ELEMENT_NAME + "/pipelines/descendants/descendant");
			}catch(Exception e) {}
		if (pipelinesNode != null) {
			Iterator pipelinesNodeIter = pipelinesNode.iterator();
			while (pipelinesNodeIter.hasNext()) {
				XFTItem item = (XFTItem)pipelinesNodeIter.next();
				arcProject.getItem().removeItem(item);
			}
		}
		try {
			pipelinesNode = arcProject.getChildItemCollection(arcProject.SCHEMA_ELEMENT_NAME + "/pipelines/pipeline");
		}catch(Exception e) {}
		if (pipelinesNode != null) {
			Iterator pipelinesNodeIter = pipelinesNode.iterator();
			while (pipelinesNodeIter.hasNext()) {
				XFTItem item = (XFTItem)pipelinesNodeIter.next();
				arcProject.getItem().removeItem(item);
			}
		}
		}catch(Exception e) {}
*/		if (pipelinesHash != null && pipelinesHash.size() > 0) { //There are some site   pipelines
			List<ArcProjectPipelineI> projectSelectedPipelines = arcProject.getPipelines_pipeline();
			//Gather only those pipelines which have not been selected.
			ArrayList<ArcPipelinedata> additionalPipelines = new ArrayList<ArcPipelinedata>();
			ArrayList<ArcPipelinedata> pipelines = pipelinesHash.get(project.SCHEMA_ELEMENT_NAME);
			if (pipelines != null && pipelines.size() > 0) {
				for (int i = 0; i < pipelines.size(); i++) {
					String id = pipelines.get(i).getLocation();
					if (!exists(projectSelectedPipelines, id)) {
						additionalPipelines.add(pipelines.get(i));
					}
				}
				for (int i = 0; i < additionalPipelines.size(); i++) {
					rtn.setPipelines_pipeline(additionalPipelines.get(i));
				}
			}
		/*	ArrayList<XnatAbstractprotocol> studyProtocols = project.getStudyprotocol();
			if (studyProtocols.size() == 0) return arcProject;
			Hashtable<String, ArrayList<ArcPipelinedata>> additionalPipelineHash = new Hashtable<String,ArrayList<ArcPipelinedata>>();
			for (int i = 0; i < studyProtocols.size(); i++) {
				XnatAbstractprotocol protocol = studyProtocols.get(i);
				ArrayList<ArcPipelinedata> descpipelines =  pipelinesHash.get(protocol.getDataType());
				ArrayList<ArcProjectDescendantPipeline> projectSelectedDescPipelines = arcProject.getPipelinesForDescendant(protocol.getDataType());
				ArrayList<ArcPipelinedata> additionalDescPipelines = new ArrayList<ArcPipelinedata>();
				if (descpipelines != null) {
					for (int j = 0; j < descpipelines.size() ; j++) {
						String id = descpipelines.get(j).getLocation();
						if (!existsDesc(projectSelectedDescPipelines, id)) {
							additionalDescPipelines.add(descpipelines.get(j));
						}
					}
					additionalPipelineHash.put(protocol.getDataType(), additionalDescPipelines);
				}
			}*/
			Hashtable<String, ArrayList<ArcPipelinedata>> additionalPipelineHash = new Hashtable<String,ArrayList<ArcPipelinedata>>();
			Enumeration<String> keys = pipelinesHash.keys();
			while (keys.hasMoreElements()) {
				String xsiType = keys.nextElement();
				ArrayList<ArcPipelinedata> descpipelines =  pipelinesHash.get(xsiType);
				List<ArcProjectDescendantPipelineI> projectSelectedDescPipelines = arcProject.getPipelinesForDescendant(xsiType);
				ArrayList<ArcPipelinedata> additionalDescPipelines = new ArrayList<ArcPipelinedata>();
				if (descpipelines != null) {
					for (int j = 0; j < descpipelines.size() ; j++) {
						String id = descpipelines.get(j).getLocation();
						if (!existsDesc(projectSelectedDescPipelines, id)) {
							additionalDescPipelines.add(descpipelines.get(j));
						}
					}
					additionalPipelineHash.put(xsiType, additionalDescPipelines);
				}
			}
			Enumeration<String> dataTypes = additionalPipelineHash.keys();
			while (dataTypes.hasMoreElements()) {
				String dataType = dataTypes.nextElement();
				ArrayList<ArcPipelinedata> additionalDescPipelines = additionalPipelineHash.get(dataType);
				ArcProjectDescendant arcProjectDesc = new ArcProjectDescendant();
				arcProjectDesc.setXsitype(dataType);
				for (int i = 0; i < additionalDescPipelines.size(); i++) {
						arcProjectDesc.setPipeline(additionalDescPipelines.get(i));
				}
				rtn.setPipelines_descendants_descendant(arcProjectDesc);
			}
		}
		return rtn;
	}

	public List getAdditionalPipelinesForDatatype(XnatProjectdata project, String schemaType) throws Exception{
		List rtn = new ArrayList();
		ArcProject arcProject = getAdditionalPipelines(project);
		if (arcProject == null) return rtn;
		if (schemaType.equals(project.SCHEMA_ELEMENT_NAME)) {
			rtn = arcProject.getPipelines_pipeline();
		}else {
			ArcProjectDescendant desc = arcProject.getDescendant(schemaType);
			if (desc == null) {
				return rtn;
			}
			rtn = desc.getPipeline();
		}
		return rtn;
	}


	public XFTTable getResultsTable() {
		XFTTable rtn = new XFTTable();
		ArrayList<String> cols = new ArrayList<String>();
		cols.add("DataType");
		cols.add("Generates"); cols.add("Description"); cols.add("Site");
		rtn.initTable(cols);
		Hashtable<String, ArrayList<PipePipelinedetails>> sitePipelines = getPipelinesPerDataType();
		Enumeration<String> keys = sitePipelines.keys();
		while (keys.hasMoreElements()) {
			String dataType = keys.nextElement();
			ArrayList<PipePipelinedetails> pipelines = sitePipelines.get(dataType);
			for (int i = 0; i < pipelines.size(); i++) {
				String[] rowEntries = new String[4];
				rowEntries[0] =  getDisplayName(dataType);
				rowEntries[1] = getElementsGeneratedBy(pipelines.get(i));
				rowEntries[2] = pipelines.get(i).getDescription();
				rowEntries[2] = "Site";
				rtn.insertRow(rowEntries);
			}
		}
		return rtn;
	}




}
