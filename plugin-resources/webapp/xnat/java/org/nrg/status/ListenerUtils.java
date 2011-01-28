package org.nrg.status;

import java.util.concurrent.Callable;

public class ListenerUtils {

	
	public static <T extends StatusProducer & Callable> T addListeners(StatusProducer src,T dest){
		for(final StatusListenerI listener: src.getListeners()){
			dest.addStatusListener(listener);
		}
		return dest;
	}
}
