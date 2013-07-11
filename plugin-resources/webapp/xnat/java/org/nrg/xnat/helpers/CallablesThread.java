/*
 * org.nrg.xnat.helpers.CallablesThread
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class CallablesThread<A extends Object> extends Thread {
	List<Callable<A>> actions=new ArrayList<Callable<A>>();
	
	public CallablesThread(){
		
	}
	
	public void addCallable(final Callable<A> call){
		actions.add(call);
	}
	
	@Override
	public void run() {
		super.run();
	}

}
