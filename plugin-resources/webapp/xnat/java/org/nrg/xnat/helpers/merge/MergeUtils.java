package org.nrg.xnat.helpers.merge;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatResourceI;
import org.nrg.xdat.model.XnatResourceseriesI;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class MergeUtils {

	public static boolean compareResources(final XnatAbstractresourceI src, final XnatAbstractresourceI dest){
		if(src instanceof XnatResourceseriesI){
			return (((XnatResourceseriesI)src).getPath()+((XnatResourceseriesI)src).getPattern()).equals(((XnatResourceseriesI)src).getPath()+((XnatResourceseriesI)src).getPattern());
		}else{
			return ((XnatResourceI)src).getUri().equals(((XnatResourceI)dest).getUri());
		}
	}

	protected static XnatImagescandataI getMatchingScan(final XnatImagescandataI scan, final List<XnatImagescandataI> list){
		try {
		return Iterables.find(list,new Predicate<XnatImagescandataI>(){
			@Override
			public boolean apply(XnatImagescandataI scan2) {
					return StringUtils.equals(scan.getId(), scan2.getId());
			}}
		);
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	protected static XnatAbstractresourceI getMatchingResource(final XnatAbstractresourceI res, List<XnatAbstractresourceI> list){
		try {
		return Iterables.find(list,new Predicate<XnatAbstractresourceI>(){
			@Override
			public boolean apply(XnatAbstractresourceI res2) {
					return StringUtils.equals(res.getLabel(),res2.getLabel());
			}}
		);
		} catch (NoSuchElementException e) {
			return null;
		}
	}
	
	
}
