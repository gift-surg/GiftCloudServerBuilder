package org.nrg.status;

import java.util.Collection;
import java.util.concurrent.Callable;

@SuppressWarnings("rawtypes")
public class ListenerUtils {

	
	public static <T extends StatusProducer & Callable> T addListeners(StatusProducer src,T dest){
		return addListeners(src.getListeners(),dest);
	}
	
	public static <T extends StatusProducer & Callable> T addListeners(Collection<StatusListenerI> src,T dest){
		for(final StatusListenerI listener: src){
			dest.addStatusListener(listener);
		}
		return dest;
	}
}
