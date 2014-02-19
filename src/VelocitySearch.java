import java.io.File;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Hashtable;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTTool;
import org.nrg.xft.commandPrompt.CommandPromptTool;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.utils.StringUtils;

/*
 * VelocitySearch
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/1/13 9:12 AM
 */



/**
 * @author Tim
 *
 */
public class VelocitySearch  extends CommandPromptTool {
    public VelocitySearch(String[] args)
	{
	    super(args);
	}
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "VelocitySearch";
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getDescription()
     */
    public String getDescription() {
        return "Retrieves Velocity based summary from XNAT Datastore";
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getAdditionalUsageInfo()
     */
    public String getAdditionalUsageInfo() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#process()
     */
    public void process() {
        Hashtable hash = variables;
	    int _return = 0;
		try {
		    if (XFT.VERBOSE)System.out.println("Using Local Processing");
		    File f = new File("");
		    String dir = f.getAbsolutePath();
			
			if (hash.get("dir") != null)
				dir = (String)hash.get("dir");
			
			if (! dir.endsWith(File.separator))
				dir += File.separator;
			
			f = new File(dir);
			if (!f.exists())
			{
			    f.mkdir();
			}
			
			
			String comparison = "=";
			if (hash.get("comparison")!=null)
			{
			    comparison = (String)hash.get("comparison");
			}
			
			String elementName = StringUtils.GetRootElementName((String)hash.get("field"));
			
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

			String field = (String)hash.get("field");

			String fieldElementName = StringUtils.GetRootElementName((String)hash.get("field"));
			String validElementName = XFTTool.GetValidElementName(fieldElementName);
			if (!validElementName.equals(fieldElementName))
			{
			    field = validElementName + XFT.PATH_SEPERATOR + StringUtils.GetFieldText(field);
			}
			
			Object o = hash.get("value");
	
			if (XFT.VERBOSE)System.out.println("DATA TYPE:" + elementName);
			if (XFT.VERBOSE)System.out.println("FIELD:" + field);
			if (XFT.VERBOSE)System.out.println("VALUE:" + o);
			if (XFT.VERBOSE)System.out.println("COMPARISON:" + comparison);
			
			tool.info("\n\nNEW SEARCH (" + tool.getUser().getUsername() + ")");
			tool.info("DATA TYPE:" + elementName);
			tool.info("FIELD:" + field);
			tool.info("VALUE:" + o);
			tool.info("COMPARISON:" + comparison);
			
			int i= tool.VelocitySearch(elementName,field, comparison,o);
		    try {
	            XFT.closeConnections();
	        } catch (SQLException e1) {
	        }
		    System.exit(i);
			
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
			_return= 4;
		} catch (XFTInitException e) {
			e.printStackTrace();
			_return= 5;
		} catch (SQLException e) {
			e.printStackTrace();
			_return= 6;
		} catch (DBPoolException e) {
			e.printStackTrace();
			_return= 7;
		} catch (FieldNotFoundException e) {
			e.printStackTrace();
			_return= 8;
		} catch (Exception e) {
			e.printStackTrace();
			_return= 9;
		}finally{
		    try {
	            XFT.closeConnections();
	        } catch (SQLException e1) {
	        }
		}
		System.exit(_return);
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        addPossibleVariable("field","field to search on - Must be specified using dot syntax from the parent element. (i.e. ClinicalAssessment.neuro.CDR.memory",true);
        addPossibleVariable("value","value to search for.",new String[]{"v","value"},true);
        addPossibleVariable("comparison","(i.e. '=','<','<=','>','>=', or 'LIKE').",new String[]{"c","comparison"},false);
        addPossibleVariable("dataType","Root level data type to return.",new String[]{"e","dataType"},false);
        addPossibleVariable("dir","Directory to store created files.",false);
    }

    public static void main(String[] args) {
        VelocitySearch b = new VelocitySearch(args);	
		return;
    }

    
	public String getService(){
	  return "axis/VelocitySearch.jws";
	}
	
	public void service()
	{
	    if (XFT.VERBOSE)System.out.println("Using Web Service");
	    
	    File f = new File("");
	    String dir = f.getAbsolutePath();
		
		if (variables.get("dir") != null)
			dir = (String)variables.get("dir");
		
		if (! dir.endsWith(File.separator))
			dir += File.separator;
		
		f = new File(dir);
		if (!f.exists())
		{
		    f.mkdir();
		}
	    int _return = 0;
	    try {
            Service service = new Service();
            Call call = (Call)service.createCall();
            call.setTargetEndpointAddress(url);
            
            call.setOperationName("search");
            
            String user = (String)variables.get("username");
            String pass = (String)variables.get("password");
            String field =(String)variables.get("field");
            String comparison = (String)variables.get("comparison");
            String value = (String)variables.get("value");
            String dataType = (String)variables.get("dataType");

            call.setUsername(user);
            call.setPassword(pass);
            Object[] params = {field,comparison,value,dataType};
            

    	    if (XFT.VERBOSE)System.out.println("Sending Request...");
    	    long startTime = Calendar.getInstance().getTimeInMillis();
            String o = (String)call.invoke(params);
    	    long duration = Calendar.getInstance().getTimeInMillis() - startTime;
    	    if (XFT.VERBOSE)System.out.println("Response Received (" + duration + " ms)\n\n");

    	    System.out.println(o);
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
