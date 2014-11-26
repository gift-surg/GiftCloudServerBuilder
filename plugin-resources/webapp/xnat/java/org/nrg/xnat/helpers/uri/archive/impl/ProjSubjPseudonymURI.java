/**
 * 
 */
package org.nrg.xnat.helpers.uri.archive.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveURI;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatReconstructedimagedataI;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xnat.helpers.uri.archive.PseudonymURII;
import org.nrg.xnat.turbine.utils.ArchivableItem;

import com.google.common.collect.Lists;

/**
 * @author dzhoshkun
 *
 */
public class ProjSubjPseudonymURI extends ProjSubjURI implements PseudonymURII {
	protected ExtSubjectpseudonym pseudonym = null;

	public ProjSubjPseudonymURI(Map<String, Object> props, String uri) {
		super(props, uri);
	}

	@Override
	public ExtSubjectpseudonym getPseudonym() {
		populatePseudonym();
		return pseudonym;
	}

	// TODO ___ include anything pertaining to project in pseudonym extraction
	protected void populatePseudonym() {
		super.populateSubject();
		
		if(pseudonym==null){
			final String pseudonymID= (String)props.get(URIManager.PSEUDONYM_ID);
			
//			if(pseudonym==null){
//				pseudonym=ExtSubjectpseudonym.getExtSubjectpseudonymsByExtSubjectpseudonymId(pseudonymID, null, false);
//			}
			if(pseudonym==null&& pseudonymID!=null){
				CriteriaCollection cc= new CriteriaCollection("AND");
				cc.addClause("ext:subjectPseudonym/ID", pseudonymID);
				cc.addClause("ext:subjectPseudonym/pseudonymized_subject_ID", super.getSubject().getId());
				ArrayList<ExtSubjectpseudonym> pseudonyms=ExtSubjectpseudonym.getExtSubjectpseudonymsByField(cc, null, false);
				if(pseudonyms.size()>0){
					pseudonym=pseudonyms.get(0);
				}
			}
		}
	}

}
