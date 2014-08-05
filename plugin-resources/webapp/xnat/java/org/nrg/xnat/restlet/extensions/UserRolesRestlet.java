package org.nrg.xnat.restlet.extensions;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.xdat.om.XdatRoleType;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.security.services.RoleRepositoryServiceI.RoleDefinitionI;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import com.google.common.collect.Lists;

/**
 * @author tim@deck5consulting.com
 *
 * Implementation of the User Roles functionality.  The post method adds or removes role for the specified user.
 */
@XnatRestlet("/user/{USER_ID}/roles")
public class UserRolesRestlet extends SecureResource {
    static Logger logger = Logger.getLogger(UserRolesRestlet.class);
	XDATUser other=null;
	String userId=null;
	/**
	 * @param context standard
	 * @param request standard
	 * @param response standard
	 */
	public UserRolesRestlet(Context context, Request request, Response response) {
		super(context, request, response);
		
		if (!user.isSiteAdmin()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "User does not have privileges to access this project.");
        } else {
            userId = (String) getRequest().getAttributes().get("USER_ID");
            if (StringUtils.isBlank(userId)) {
                getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "As of this release, you must specify a user on which to perform.");
                return;
            }else{
            	try {
					other=new XDATUser(userId);
				} catch (Exception e) {
					logger.error("",e);
				}
            }
            this.getVariants().add(new Variant(MediaType.ALL));
        }
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public void handlePost() {
		final List<String> roles=Lists.newArrayList();
		
		if(hasQueryVariable("roles")){
			final String roleS=getQueryVariable("roles");
			
			for(String role: roleS.split(",")){
				roles.add(role);
			}
		}
		
		try {
			//relies on a configuration parameter which defines the available roles.
			Collection<RoleDefinitionI> defined=Roles.getRoles();
			
			final List<String> allDefinedRoles=Lists.newArrayList();
			for(RoleDefinitionI def:defined){
				allDefinedRoles.add(def.getKey());
			}
			
	        //remove roles and save one at a time so that there is a separate workflow entry for each one
			for(String dRole:allDefinedRoles){
				if(!roles.contains(dRole)){
					//remove role if it is there
					if(other.checkRole(dRole)){
						other.deleteRole(user, dRole);
						
						other=new XDATUser(userId);//get fresh db copy
					}
				}
			}
			
	        //add roles and save one at a time so that there is a separate workflow entry for each one
			for(String dRole:allDefinedRoles){
				if(roles.contains(dRole)){
					//add role if isn't there
					if(!other.checkRole(dRole)){
						other.addRole(user, dRole);
						
						other=new XDATUser(userId);//get fresh db copy
					}
				}
			}
				
			this.getResponse().setEntity(new StringRepresentation(""));
			this.getResponse().setStatus(Status.SUCCESS_ACCEPTED);
		} catch (Throwable e) {
			logger.error("",e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
			return;
		}
	}
	


	@Override
	public Representation represent(Variant variant) throws ResourceException {	
		MediaType mt = overrideVariant(variant);
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		
		XFTTable table=new XFTTable();
		table.initTable(new String[]{"role"});
		for(XdatRoleType role:other.getAssignedRoles_assignedRole()){
			table.rows().add(new Object[]{role.getRoleName()});
		}
		
		if(table!=null)params.put("totalRecords", table.size());
		return this.representTable(table, mt, params);
	}
}
