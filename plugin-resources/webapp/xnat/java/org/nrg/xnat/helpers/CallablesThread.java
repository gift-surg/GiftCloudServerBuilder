package org.nrg.xnat.helpers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class CallablesThread extends Thread {
	List<Callable<List<String>>> actions=new ArrayList<Callable<List<String>>>();
	
	public CallablesThread(){
		
	}
	
	public void addCallable(final Callable<List<String>> call){
		actions.add(call);
	}
	
	@Override
	public void run() {
		super.run();
	}

}
