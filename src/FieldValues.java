import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Hashtable;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.search.CriteriaCollection;
import org.nrg.xdat.search.QueryOrganizer;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTTable;
import org.nrg.xft.XFTTool;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.utils.StringUtils;

/*
 * Created on Apr 19, 2006
 *
 */

/**
 * @author Tim
 *
 */
public class FieldValues   extends CommandPromptTool{
	
//	java -Xmx256M -classpath .;XFT.jar;commons-collections-2.0.jar;commons-configuration-1.0-dev.jar;commons-dbcp-1.0-dev-20020806.jar;commons-lang-1.0.jar;commons-pool-1.0.jar;jakarta-regexp-1.2.jar;log4j-1.2.6.jar;parser.jar;pg74.214.jdbc2.jar;stratum-1.0-b3.jar;xerces-J_1.4.0.jar XFTApp
    public FieldValues(String[] args)
	{
	    super(args);
	}
    
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getAdditionalUsageInfo()
     */
    public String getAdditionalUsageInfo() {
        return "";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getDescription()
     */
    public String getDescription() {
        return "Function used to access particular sub-sets of the data in the database.\n";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "Search";
    }
	
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        addPossibleVariable("rfield","field values to return - Must be specified using dot syntax from the parent element. (i.e. ClinicalAssessment.ID",true);
        addPossibleVariable("sfield","field to search on - Must be specified using dot syntax from the parent element. (i.e. ClinicalAssessment.neuro.CDR.memory",true);
        addPossibleVariable("value","value to search for.",new String[]{"v","value"},true);
        addPossibleVariable("comparison","(i.e. '=','<','<=','>','>=', or 'LIKE').",new String[]{"c","comparison"},false);
        addPossibleVariable("order","(i.e. 'ASC' OR 'DESC').",new String[]{"order"},false);
    }
    
    public static void main(String[] args) {
        FieldValues b = new FieldValues(args);	
		return;
	}	
    
	public  void process()
	{
	    Hashtable hash = variables;
	    int _return = 0;
		try {
		    			
			String elementName = StringUtils.GetRootElementName((String)hash.get("rfield"));
			
			boolean valid = XFTTool.ValidateElementName(elementName);
			if (! valid)
			{
				System.out.println("\nERROR:  Invalid Element '" + elementName + "'");
				tool.close();
				System.exit(3);
			}

			
			if (hash.get("dataType")!=null)
			{
			    elementName = (String)hash.get("dataType");
			}
			
			valid = XFTTool.ValidateElementName(elementName);
			if (! valid)
			{
				System.out.println("\nERROR:  Invalid Element '" + elementName + "'");
				tool.close();
				System.exit(3);
			}

			String rfield = (String)hash.get("rfield");
			rfield = org.nrg.xft.utils.StringUtils.StandardizeXMLPath(rfield);

			String rfieldElementName = StringUtils.GetRootElementName((String)hash.get("rfield"));
			String rvalidElementName = XFTTool.GetValidElementName(rfieldElementName);
			if (!rvalidElementName.equals(rfieldElementName))
			{
			    rfield = rvalidElementName + XFT.PATH_SEPERATOR + StringUtils.GetFieldText(rfield);
			}

			String sfield = (String)hash.get("sfield");
			sfield = org.nrg.xft.utils.StringUtils.StandardizeXMLPath(sfield);

			String sfieldElementName = StringUtils.GetRootElementName((String)hash.get("sfield"));
			String svalidElementName = XFTTool.GetValidElementName(sfieldElementName);
			if (!svalidElementName.equals(sfieldElementName))
			{
			    sfield = svalidElementName + XFT.PATH_SEPERATOR + StringUtils.GetFieldText(sfield);
			}
			

			
			String comparison = "=";
			if (hash.get("comparison")!=null)
			{
			    comparison = (String)hash.get("comparison");
			}
			
			Object o = hash.get("value");
	
			QueryOrganizer qo = new QueryOrganizer(elementName,this.getUser(),ViewManager.ALL);
			qo.addField(rfield);
			qo.addField(sfield);
			
			CriteriaCollection cc =new CriteriaCollection("AND");
			cc.addClause(sfield,comparison,o);

			String query = "SELECT * FROM (" + qo.buildQuery() + ") SEARCH";
			query += " WHERE " + cc.getSQLClause(qo);
			query += ";";
			
			XFTTable table = XFTTable.Execute(query,SchemaElement.GetElement(elementName).getDbName(),this.getUser().getLogin());
			
			
			String colname= qo.translateXMLPath(rfield);
						
			if (hash.get("order")!=null)
			{
			    if (((String)hash.get("order")).equalsIgnoreCase("DESC"))
			    {
			        table.sort(colname,"DESC");
			    }else if (((String)hash.get("order")).equalsIgnoreCase("ASC"))
			    {
			        table.sort(colname,"ASC");
			    }else{
			        System.out.println("\nERROR: -order value must be 'ASC' or 'DESC'");
			    }
			}else{
				table.sort(colname,"ASC");
			}
			
			table.resetRowCursor();
			while (table.hasMoreRows())
			{
			    Hashtable row = table.nextRowHash();
			    System.out.println(row.get(colname.toLowerCase()));
			}
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
			_return= 4;
		} catch (XFTInitException e) {
			e.printStackTrace();
			_return= 5;
		} catch (SQLException e) {
			e.printStackTrace();
			_return= 6;
		} catch (Exception e) {
		    if (e.toString().startsWith("java.lang.Exception: Unable to connect"))
		    {
		        System.out.println("\n" + e.getMessage());
		        System.out.println("Please try an intermediary data type.");
		        _return= 9;
		    }else{
				e.printStackTrace();
				_return= 9;
		    }
		}finally{
		    try {
	            XFT.closeConnections();
	        } catch (SQLException e1) {
	        }
		}
		System.exit(_return);
	}
	
	public String getService(){
		  return "axis/FieldValues.jws";
		}
		
		public void service()
		{
		    if (XFT.VERBOSE)System.out.println("Using Web Service");
		  
		    int _return = 0;
		    try {
	            Service service = new Service();
	            Call call = (Call)service.createCall();
	            call.setTargetEndpointAddress(url);
	            
	            call.setOperationName("search");
	            
	            String user = (String)variables.get("username");
	            String pass = (String)variables.get("password");
	            String field =(String)variables.get("sfield");
	            String comparison = (String)variables.get("comparison");
	            String value = (String)variables.get("value");
	            String rfield = (String)variables.get("rfield");
	            String order = (String)variables.get("order");

	            call.setUsername(user);
	            call.setPassword(pass);
	            Object[] params = {field,comparison,value,rfield,order};
	            

	    	    if (XFT.VERBOSE)System.out.println("Sending Request...");
	    	    long startTime = Calendar.getInstance().getTimeInMillis();
	            Object[] o = (Object[])call.invoke(params);
	    	    long duration = Calendar.getInstance().getTimeInMillis() - startTime;
	    	    if (XFT.VERBOSE)System.out.println("Response Received (" + duration + " ms)");
	    	    
	            for (int i =0;i<o.length;i++)
	            {
	                System.out.println(o[i]);
	            }
	            
	            //String doc = soapService.getFileAsAttachment(xml);

				
//	            org.apache.axis.attachments.Attachments attachments= call.getResponseMessage().getAttachmentsImpl();
//	            Iterator iter = attachments.getAttachments().iterator();
//	            while (iter.hasNext())
//	            {
//	                	try {
//                            AttachmentPart attachPart = (AttachmentPart) iter.next();
//                            DataHandler arrow = attachPart.getDataHandler();
//                            File myFile1 = new File("C:\\temp\\" + attachPart.getContentId() + ".gif");
//                            FileOutputStream myFOS1 = new FileOutputStream(myFile1);
//                            arrow.writeTo(myFOS1);
//                            System.out.println(attachPart.getContentId());
//                        } catch (Exception e) {
//                            System.err.print(e);
//                        }
//	            }
				
				
			

	        }catch(AxisFault ex2)
	        {
	            Throwable e = ex2.detail;
	            if (e instanceof ElementNotFoundException) {
	                e.printStackTrace();
	                _return= 4;
	            }else if (e instanceof XFTInitException) {
	                e.printStackTrace();
	                _return= 5;
	            }else if (e instanceof SQLException) {
	                e.printStackTrace();
	                _return= 6;
	            }else if (e instanceof DBPoolException) {
	                e.printStackTrace();
	                _return= 7;
	            }else if (e instanceof FieldNotFoundException) {
	                e.printStackTrace();
	                _return= 8;
	            }else if (e instanceof Exception) {
	                e.printStackTrace();
	                _return= 9;
				}else{
				    System.out.println(ex2.getFaultString());
	                _return= 10;
				}
	        }catch (RemoteException ex) {
	            
	            Throwable e = ex.getCause();
	            if (e instanceof ElementNotFoundException) {
	                e.printStackTrace();
	                _return= 4;
	            }else if (e instanceof XFTInitException) {
	                e.printStackTrace();
	                _return= 5;
	            }else if (e instanceof SQLException) {
	                e.printStackTrace();
	                _return= 6;
	            }else if (e instanceof DBPoolException) {
	                e.printStackTrace();
	                _return= 7;
	            }else if (e instanceof FieldNotFoundException) {
	                e.printStackTrace();
	                _return= 8;
	            }else if (e instanceof Exception) {
	                e.printStackTrace();
	                _return= 9;
				}else{
				    ex.printStackTrace();
	                _return= 10;
				}
	        } catch (ServiceException ex) {
	            ex.printStackTrace();
				_return= 11;
	        }
			System.exit(_return);
		}
}
