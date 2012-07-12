// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * Created on Apr 24, 2006
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.IOException;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.DisplayItemAction;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.exception.ValidationException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.schema.design.SchemaElementI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.XMLValidator;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xft.utils.ValidationUtils.XFTValidator;
import org.xml.sax.SAXParseException;

/**
 * @author Tim
 * 
 */
public class XMLUpload extends SecureAction {
	static org.apache.log4j.Logger logger = Logger.getLogger(XMLUpload.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.turbine.modules.actions.VelocityAction#doPerform(org.apache
	 * .turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	public void doPerform(RunData data, Context context) throws Exception {
		// get the ParameterParser from RunData
		ParameterParser params = data.getParameters();

		// grab the FileItems available in ParameterParser
		FileItem fi = params.getFileItem("xml_to_store");

		String allowDeletion = (String) TurbineUtils.GetPassedParameter(
				"allowdeletion", data);
		if (fi != null && allowDeletion != null) {
			XFTItem item = null;
			try {
				XMLValidator validator = new XMLValidator();
				XMLValidator.ValidationHandler handler = validator
						.validateInputStream(fi.getInputStream());

				if (!handler.assertValid()) {
					throw handler.getErrors().get(0);
				}

				// Document doc = XMLUtils.GetDOM(fi.getInputStream());
				// item =
				// XMLReader.TranslateDomToItem(doc,TurbineUtils.getUser(data));
				SAXReader reader = new SAXReader(TurbineUtils.getUser(data));
				item = reader.parse(fi.getInputStream());
				if (XFT.VERBOSE)
					System.out.println("Loaded XML Item:"
							+ item.getProperName());
				logger.info("Loaded XML Item:" + item.getProperName());

				ValidationResults vr = XFTValidator.Validate(item);
				if (vr.isValid()) {
					if (XFT.VERBOSE)
						System.out.println("Validation: PASSED");
					logger.info("Validation: PASSED");

					boolean q;
					boolean override;
					q = item.getGenericSchemaElement().isQuarantine();
					override = false;

					PersistentWorkflowI wrk = null;
					if (item.getItem().instanceOf("xnat:experimentData")
							|| item.getItem().instanceOf("xnat:subjectData")) {
						wrk = PersistentWorkflowUtils.buildOpenWorkflow(
								TurbineUtils.getUser(data),
								item.getItem(),
								newEventInstance(data,
										EventUtils.CATEGORY.SIDE_ADMIN,
										EventUtils.STORE_XML));
					}

					final EventMetaI ci;
					if (wrk != null) {
						ci = wrk.buildEvent();
					} else {
						ci = EventUtils.ADMIN_EVENT(TurbineUtils.getUser(data));
					}

					SaveItemHelper.unauthorizedSave(item,
							TurbineUtils.getUser(data), false, q, override,
							allowDeletion.equalsIgnoreCase("true"), ci);

					if (wrk != null) {
						PersistentWorkflowUtils.complete(wrk, ci);

					}

					if (XFT.VERBOSE) {

						System.out.println("Item Successfully Stored.");
						logger.info("Item Successfully Stored.");

					}
					// Here is where my change was
					//XFTItem item = populater.getItem();
		            XnatProjectdata  project = new XnatProjectdata(item);
		        	XDATUser user = TurbineUtils.getUser(data);
		        	final PersistentWorkflowI wrk2=PersistentWorkflowUtils.getOrCreateWorkflowData(null, user, project.SCHEMA_ELEMENT_NAME,project.getId(),project.getId(),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ADMIN));
			    	EventMetaI c=wrk2.buildEvent();
		        	SaveItemHelper.authorizedSave(item,user, false, false,c);
		        	XnatProjectdata postSave = new XnatProjectdata(item);
		            postSave.getItem().setUser(user);
		            
		            postSave.initGroups(c);
		            
		            user.initGroups();
		        	
		    		user.clearLocalCache();
		    		//end change here
		    		
					DisplayItemAction dia = new DisplayItemAction();
					data = TurbineUtils.SetSearchProperties(data, item);
					dia.doPerform(data, context);

					postProcessing(item, data, context);

					return;
				} else {
					throw new ValidationException(vr);
				}
			} catch (IOException e) {
				logger.error("", e);
				data.setScreenTemplate("Error.vm");
				data.setMessage("Error loading document.");
			} catch (XFTInitException e) {
				logger.error("", e);
			} catch (ElementNotFoundException e) {
				logger.error("", e);
			} catch (FieldNotFoundException e) {
				logger.error("", e);
			} catch (ValidationException e) {
				logger.error("", e);
				data.setScreenTemplate("Error.vm");
				data.setMessage("XML Validation Exception.<BR>"
						+ e.VALIDATION_RESULTS.toHTML());
			} catch (Exception e) {
				if (e instanceof SAXParseException) {
					logger.error("", e);
					data.setScreenTemplate("Error.vm");
					String message = "SAX Parser Exception.<BR><BR>"
							+ e.getMessage();
					data.setMessage(message);
				} else if (e instanceof InvalidPermissionException) {
					logger.error("", e);
					data.setScreenTemplate("Error.vm");
					String message = "Permissions Exception.<BR><BR>"
							+ e.getMessage();
					final SchemaElement se = SchemaElement.GetElement(item
							.getXSIType());
					final ElementSecurity es = se.getElementSecurity();
					if (es != null && es.getSecurityFields() != null) {
						message += "<BR><BR>Please review the security field ("
								+ se.getElementSecurity().getSecurityFields()
								+ ") for this data type.";
						message += " Verify that the data reflects a currently stored value and the user has relevant permissions for this data.";
					}
					data.setMessage(message);
				} else {
					logger.error("", e);
					data.setScreenTemplate("Error.vm");
					data.setMessage(e.getMessage());
				}
			}
			
		}
	}

	public void postProcessing(XFTItem item, RunData data, Context context)
			throws Exception {
		SchemaElementI se = SchemaElement.GetElement(item.getXSIType());
		if (se.getGenericXFTElement().getType().getLocalPrefix()
				.equalsIgnoreCase("xdat")) {
			ElementSecurity.refresh();
		} else if (se.getFullXMLName().equals("xnat:investigatorData")
				|| se.getFullXMLName().equals("xnat:projectData")) {
			ElementSecurity.refresh();
		}
	}
}
