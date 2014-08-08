package org.nrg.xnat.restlet.projectsList.extensions;

import java.util.Hashtable;

import org.nrg.xft.XFTTable;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.resources.SecureResource.FilteredResourceHandlerI;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class EditableProjects implements FilteredResourceHandlerI{

	@Override
	public boolean canHandle(SecureResource resource) {
		return resource.isQueryVariableTrue("creatableTypes");
	}

	@Override
	public Representation handle(SecureResource resource, Variant variant) throws Exception {
		StringBuilder builder=new StringBuilder();
        if(resource.user.getGroup("ALL_DATA_ADMIN")!=null){
        	 builder.append("SELECT ID FROM xnat_projectData;");
        }else{
        	 builder.append(String.format("SELECT DISTINCT xfm.field_value AS ID FROM xdat_user_groupID map JOIN xdat_userGroup gp ON map.groupid=gp.id JOIN xdat_element_access xea ON gp.xdat_usergroup_id=xea.xdat_usergroup_xdat_usergroup_id JOIN xdat_field_mapping_set xfms ON xea.xdat_element_access_id=xfms.permissions_allow_set_xdat_elem_xdat_element_access_id JOIN xdat_field_mapping xfm ON xfms.xdat_field_mapping_set_id=xfm.xdat_field_mapping_set_xdat_field_mapping_set_id AND create_element=1 AND field_value!='*' AND field_value!='' and field !='' WHERE map.groups_groupid_xdat_user_xdat_user_id=%s",resource.user.getXdatUserId()));
        	 if(resource.hasQueryVariable("data-type")){
        		 GenericWrapperElement gwe = GenericWrapperElement.GetElement(resource.getQueryVariable("data-type"));
        		 if(gwe!=null){
        			 builder.append(" AND xea.element_name='" + gwe.getXSIType() + "' ");
        		 }
        	 }
        }
   
        return resource.representTable(XFTTable.Execute(builder.toString(), resource.user.getDBName(), resource.userName), resource.overrideVariant(variant), new Hashtable<String,Object>()) ;
	}

}
