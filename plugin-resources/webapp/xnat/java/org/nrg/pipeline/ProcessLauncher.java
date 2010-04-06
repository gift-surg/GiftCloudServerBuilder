/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.pipeline;

import org.apache.log4j.Logger;

public class ProcessLauncher extends Thread{

    static org.apache.log4j.Logger logger = Logger.getLogger(ProcessLauncher.class);
    String command;
    boolean success;
    

    public void setCommand(String cmd) {
        command = cmd;
    }
    
    public boolean getExitStatus() {
        return success;
    }
    
    public void run() {
        StreamGobbler errorGobbler = null;
        StreamGobbler outputGobbler = null;
        Process proc = null;
        try {
            String cmdArray[] = getCommandArray();
            proc = Runtime.getRuntime().exec(cmdArray);
            success = true;
            errorGobbler = new 
            StreamGobbler(proc.getErrorStream(), "ERROR"); 
            errorGobbler.log("Executing: " + command);
            // any output?
            outputGobbler = new 
                StreamGobbler(proc.getInputStream(), "OUTPUT");
            outputGobbler.log("Executing: " + command + "\n");
            // kick them off
            errorGobbler.start();
            outputGobbler.start();
            int exitValue = proc.waitFor();
            success = (exitValue==0)?true:false;
            if (!success) {
              logger.error(" Couldnt launch " + command );
            }
        }catch(Exception e) {
            logger.error(e.getMessage() + " for command " + command,e);
            success = false;
        }finally {
            if (proc != null) proc.destroy();
            if (errorGobbler != null) errorGobbler.finish();
            if (outputGobbler != null) outputGobbler.finish();
        }
    }
    
    public String[] getCommandArray() {
        String cmdArray[] = null;
        String osName = System.getProperty("os.name");
        if (osName.toUpperCase().indexOf("WINDOWS") == -1) {
            cmdArray = new String[] {"/bin/sh", "-c", command};
        }else {
            if( osName.equals( "Windows 95" ) )
                cmdArray = new String[]{"command.com","/C",command};
            else
                cmdArray = new String[]{"cmd.exe","/C",command};
        }
        return cmdArray;
    }
    
}
