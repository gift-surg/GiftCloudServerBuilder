package org.nrg.xnat.itemBuilders;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTTable;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.presentation.FlattenedItem.FlattenedItemModifierI;
import org.nrg.xft.presentation.FlattenedItemA;
import org.nrg.xft.presentation.FlattenedItemA.ItemObject;
import org.nrg.xft.presentation.FlattenedItemI;
import org.nrg.xft.presentation.ItemMerger;
import org.nrg.xft.presentation.ItemPropBuilder;
import org.nrg.xft.utils.DateUtils;
import org.nrg.xnat.itemBuilders.WorkflowBasedHistoryBuilder.WorkflowView;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA.ChangeSummary;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA.ChangeSummary.ChangeSummaryGroup;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA.DefaultBuilder;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA.EventBuilderI;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA.FieldEventI;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA.ItemEvent;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA.ItemEventI;
import org.nrg.xnat.presentation.DateBasedSummaryBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class WorkflowBasedHistoryBuilder implements Callable<Map<Number,WorkflowView>> {
    static Logger logger = Logger.getLogger(WorkflowBasedHistoryBuilder.class);
	private static final String FILE_COUNT = "file_count";
	final private ItemI i;
	final private String id;
	final private XDATUser user;
	final boolean includeFiles;
	final boolean includeDetails;
	
	public WorkflowBasedHistoryBuilder(ItemI i, String id, XDATUser user, boolean includeFiles, boolean includeDetails){
		this.i=i;
		this.id=id;
		this.user=user;
		this.includeFiles=includeFiles;
		this.includeDetails=includeDetails;
	}

	public class FileEvent extends ItemEvent implements ItemEventI{
		final int diff;
		public FileEvent(ItemObject o, String action, FieldEventI fe,
				List<ItemObject> parents, String username, int diff) {
			super(o, action, fe, parents, username);
			this.diff=diff;
		}
		
		public int getDiff(){
			return diff;
		}
	}
	
	public static Integer convertToInt(Object o){
		if(o==null){
			return 0;
		}else if(o instanceof Integer){
			return (Integer)o;
		}else {
			String s=o.toString();
			if(s.equals("")){
				return 0;
			}else{
				return Integer.valueOf(s);
			}
		}
	}
	
	final Map<Number,WorkflowView> result=new Hashtable<Number,WorkflowView>();
	public Map<Number,WorkflowView> call() throws Exception{
		if(result.size()==0){
			final Map<Date,ChangeSummary> changesByDate;
			if(includeDetails){
				final List<? extends FlattenedItemModifierI> modifiers;
				if(includeFiles){
					modifiers=Arrays.asList(new FullFileHistoryBuilder());
				}else{
					modifiers=new ArrayList<FlattenedItemModifierI>();
				}
				
				final List<FlattenedItemI> items=
					Arrays.asList(
						ItemMerger.merge(
								ItemPropBuilder.build(i.getItem(), FlattenedItemA.GET_ALL,modifiers)));
			
				final EventBuilderI builder;
				if(!includeFiles){
					builder= new DefaultBuilder(){
						public List<ItemEventI> build(ItemObject io,String action,FieldEventI fe, String username, List<ItemObject> parents, FlattenedItemI fi){
							List<ItemEventI> items=Lists.newArrayList();
							boolean _continue=true;
							if(io.getXsiTypes().contains(XnatAbstractresource.SCHEMA_ELEMENT_NAME)){
								ItemObject new_io=new ItemObject("files", "", "", Arrays.asList("system:file"));
								if(action.equals(ChangeSummaryBuilderA.MODIFIED) && fe !=null && fe.getXsiPath().equals(FILE_COUNT)){
									int _new=convertToInt(fe.getNewValue());
									int _old=convertToInt(fe.getOldValue());
									if(_new>_old){
										items.add(new FileEvent(new_io
												,ChangeSummaryBuilderA.ADDED
												,fe
												,parents,username,(_new-_old)));
										_continue=false;
									}else{
										items.add(new FileEvent(new_io
												,ChangeSummaryBuilderA.REMOVED
												,fe
												,parents,username,(_old-_new)));
										_continue=false;
									}
								}else if(!action.equals(ChangeSummaryBuilderA.MODIFIED)){
									int _new=convertToInt(fi.getFields().getParams().get(FILE_COUNT));
									if(_new>0){
										items.add(new FileEvent(new_io
												,action
												,fe
												,parents,username,(_new)));
									}
								}
							}
		
							if(_continue){
								items.addAll(super.build(io, action, fe, username, parents,fi));
							}
							
							return items;
						}
					};
				}else{
					builder=new DefaultBuilder();
				}
				changesByDate= (new DateBasedSummaryBuilder(builder)).call(items);
			}else{
				changesByDate=Maps.newHashMap();
			}
			
			final Collection<? extends PersistentWorkflowI> workflows=getWorkflows(i,id,user);
			
			final Map<Date,ChangeSummary> copy=new Hashtable<Date,ChangeSummary>(changesByDate);
			
			//merge ChangeSummaries and Workflows 
			for(final PersistentWorkflowI wrk: workflows){
				WorkflowView wv=new WorkflowView();
				wv.setWorkflow(wrk);
				for(Map.Entry<Date,ChangeSummary> cs:copy.entrySet()){
					if(cs.getValue().getNumber().equals(wrk.getWorkflowId())){
						wv.cs.add(changesByDate.remove(cs.getKey()));
					}
				}
				result.put(wrk.getWorkflowId(), wv);
			}
			
			for(ChangeSummary cs:changesByDate.values()){
				addChangeSummary(cs);
			}
		}
		
		return result;
	}
	
	public static  Collection<? extends PersistentWorkflowI> getWorkflows(ItemI i, String id,XDATUser user){
		List<PersistentWorkflowI> wrks=Lists.newArrayList();
		
		try {
			if(i.getItem().instanceOf("xnat:projectData")){
				wrks.addAll(PersistentWorkflowUtils.getWorkflowsByExternalId(user, id));
			}else{
				wrks.addAll(PersistentWorkflowUtils.getWorkflows(user, id));
				
				List<String> ids=Lists.newArrayList();
				if(i.getItem().instanceOf("xnat:imageSessionData")){
					try {
						List<List> expts=XFTTable.Execute(String.format("SELECT DISTINCT id FROM (SELECT iad.id FROM xnat_imageassessordata iad WHERE iad.id IS NOT NULL AND iad.imagesession_id='%1$s' UNION SELECT iad.id FROM xnat_imageassessordata_history iad WHERE iad.id IS NOT NULL AND iad.imagesession_id='%1$s') SRCH;",id), i.getDBName(), user.getLogin()).toArrayListOfLists();
						for(List expt:expts){
							ids.add((String)expt.get(0));
						}
					} catch (Exception e) {
						logger.error("",e);
					}
				}else if(i.getItem().instanceOf("xnat:subjectData")){
					try {
						List<List> expts=XFTTable.Execute(String.format("" +
								"SELECT DISTINCT id FROM (SELECT sad.id FROM xnat_subjectassessordata sad WHERE subject_id='%1$s' UNION " +
								"SELECT iad.id FROM xnat_subjectassessordata sad LEFT JOIN xnat_imageassessordata iad ON sad.id=iad.imagesession_id WHERE iad.id IS NOT NULL AND subject_id='%1$s' UNION " +
								"SELECT sad.id FROM xnat_subjectassessordata_history sad WHERE subject_id='%1$s' UNION " +
								"SELECT iad.id FROM xnat_subjectassessordata sad LEFT JOIN xnat_imageassessordata_history iad ON sad.id=iad.imagesession_id WHERE iad.id IS NOT NULL AND subject_id='%1$s' UNION " +
								"SELECT iad.id FROM xnat_subjectassessordata_history sad LEFT JOIN xnat_imageassessordata_history iad ON sad.id=iad.imagesession_id WHERE iad.id IS NOT NULL AND subject_id='%1$s') SRCH;",id), i.getDBName(), user.getLogin()).toArrayListOfLists();
						for(List expt:expts){
							ids.add((String)expt.get(0));
						}
					} catch (Exception e) {
						logger.error("",e);
					}
				}
				
				wrks.addAll(PersistentWorkflowUtils.getWorkflows(user, ids));
			}
		} catch (ElementNotFoundException e) {
			logger.error("",e);
		}
		
		return wrks;
	}
	
	public void addChangeSummary(ChangeSummary cs){
		if(!result.containsKey(cs.getNumber())){
			result.put(cs.getNumber(),new WorkflowView());
		}
		result.get(cs.getNumber()).cs.add(cs);
	}
	
	public class WorkflowView{
		PersistentWorkflowI workflow=null;
		List<ChangeSummary> cs=new ArrayList<ChangeSummary>();
		public PersistentWorkflowI getWorkflow() {
			return workflow;
		}
		public void setWorkflow(PersistentWorkflowI workflow) {
			this.workflow = workflow;
		}
		
		boolean sorted=false;
		public List<ChangeSummary> getChangeSummaries() {
			if(!sorted){
				Collections.sort(cs, new Comparator<ChangeSummary>() {
					@Override
					public int compare(ChangeSummary arg0,ChangeSummary arg1) {
						return DateUtils.compare(arg0.getDate(), arg1.getDate());
					}
				});
				sorted=true;
			}
			return cs;
		}
		
		public String getMessage(){
			if(workflow!=null){
				return workflow.getOnlyPipelineName();
			}else{
				return null;
			}
		}
		
		public Date getDate() throws XFTInitException, ElementNotFoundException, FieldNotFoundException, ParseException{
			if(workflow!=null){
				return workflow.getLaunchTimeDate();
			}else{
				return getChangeSummaries().get(0).getDate();
			}
		}
	
		
		public String getUsername() throws XFTInitException, ElementNotFoundException, FieldNotFoundException, ParseException{
			if(workflow!=null){
				return workflow.getUsername();
			}else{
				return getChangeSummaries().get(0).getEvents().get(0).getUsername();
			}
		}
		
		public List<String> getChangeDescription(){
			final List<String> sums=Lists.newArrayList();
			if(getChangeSummaries().size()>0){
				for(ChangeSummary cs:getChangeSummaries()){
					sums.add(cs.toString());
				}
			}
			return sums;
		}

		public String getSummary(){
			return ChangeSummary.toString(buildSummary());
		}
		
		//<dataType,Map<action,ChangeSummaryGroup>>
		public Map<String,Map<String,ChangeSummaryGroup>> buildSummary(){
			Map<String,Map<String,ChangeSummaryGroup>> sum=Maps.newHashMap();
			for(ChangeSummary cs:getChangeSummaries()){
				for(Map.Entry<String, Map<String,ChangeSummaryGroup>> outer: cs.getSummary().entrySet()){
					for(Map.Entry<String,ChangeSummaryGroup> inner: outer.getValue().entrySet()){
						String action=inner.getKey();
						String dataType=outer.getKey();
						int count=inner.getValue().getCount();
						
						if(!sum.containsKey(dataType)){
							Map<String,ChangeSummaryGroup> temp=Maps.newHashMap();
							sum.put(dataType, temp);
						}
						
						if(!sum.get(dataType).containsKey(action)){
							sum.get(dataType).put(action,new ChangeSummaryGroup(dataType,action));
						}
						
						sum.get(dataType).get(action).increment(count);
					}
				}
			}
			return sum;
		}
		
		public int getDominantChangeType(){
			int level=0;
			for(ChangeSummary cs:getChangeSummaries()){
				if(cs.getDominantChangeType()==2){
					return 2;
				}else if(cs.getDominantChangeType()==1){
					level=1;
				}
			}
			return level;
		}
	}

	public JSONObject toJSON(String dateFormat) throws Exception{
		Map<Number,WorkflowView> changes=call();

		JSONObject wrapper=new JSONObject();
		JSONArray objects=new JSONArray();
		
		for(Map.Entry<Number,WorkflowBasedHistoryBuilder.WorkflowView> entry: changes.entrySet()){
			JSONObject o = new JSONObject();
			o.put("event_id", entry.getKey());
			o.put("event_action", entry.getValue().getMessage());
			if(entry.getValue().getDate()!=null){
				if(dateFormat==null){
					o.put("event_date", entry.getValue().getDate().getTime());
				}else{
					o.put("event_date", DateUtils.format(entry.getValue().getDate(), dateFormat));
				}
			}
			o.put("event_user", entry.getValue().getUsername());
			if(entry.getValue().getWorkflow()!=null){
				o.put("event_type", entry.getValue().getWorkflow().getType());
				o.put("event_reason", entry.getValue().getWorkflow().getJustification());
				o.put("event_category", entry.getValue().getWorkflow().getCategory());
				o.put("event_status", entry.getValue().getWorkflow().getStatus());
			}
			
			JSONArray a = new JSONArray();
			o.put("changesets", a);
			for(ChangeSummaryBuilderA.ChangeSummary cs: entry.getValue().getChangeSummaries()){
				a.put(cs.toJSON(dateFormat));
			}
			
			objects.put(o);
		}
		
		wrapper.put("events", objects);
		
		return wrapper;
	}
}
