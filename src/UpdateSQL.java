import java.sql.SQLException;

import org.nrg.xdat.XDAT;
import org.nrg.xft.XFT;
import org.nrg.xft.commandPrompt.CommandPromptTool;
/*
 * Created on Mar 27, 2006
 *
 */

/**
 * @author Tim
 *
 */
public class UpdateSQL extends CommandPromptTool {

    /**
     * @param args
     */
    public UpdateSQL(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        UpdateSQL b = new UpdateSQL(args);	
		return;
	}		
	
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#definePossibleVariables()
     */
    public void definePossibleVariables() {
        this.addPossibleVariable("output","specify file location for data output 'C:\\Temp\\test.sql'","f",true);
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
        return "Function used to generate the sql create statements for all elements in the specified schemas.\n";
    }
    /* (non-Javadoc)
     * @see org.nrg.xft.commandPrompt.CommandPromptTool#getName()
     */
    public String getName() {
        return "UpdateSQL";
    }
	
	public void process()
	{
		try {
			//System.out.print(elementName + ":" + selectType + ":" + output);
		    String output = (String)variables.get("output");	
		    XDAT.GenerateUpdateSQL(output);
		
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
		    try {
                XFT.closeConnections();
            } catch (SQLException e1) {
            }
		}
		return;
	}
	
	public boolean requireLogin()
    {
       return false;
    }

}
