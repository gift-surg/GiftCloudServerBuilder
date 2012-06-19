//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jun 19, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.DeleteAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.db.PoolDBUtils;

import java.sql.SQLException;

public class DeleteXdatElementSecurity extends DeleteAction {
    protected void postDelete(RunData data, Context context) {
        final String dataType = TurbineUtils.GetPassedParameter("search_value", data).toString();
        final String query1 = "DELETE FROM xdat_element_access WHERE xdat_element_access_id IN ( select xdat_element_access_id from xdat_element_access xea LEFT JOIN xdat_element_security xes ON xea.element_name=xes.element_name WHERE xes.element_name IS NULL);";
        final String query2 = String.format("DELETE FROM xdat_meta_element WHERE element_name LIKE '%s%%';", dataType);
        final XDATUser user = TurbineUtils.getUser(data);
        String login = user.getLogin();
        try {
            PoolDBUtils.ExecuteNonSelectQuery(query1, user.getDBName(), login);
        } catch (SQLException exception) {
            _log.error("There was a SQL exception trying to remove the element access data for data type: " + dataType);
            throw new RuntimeException(exception);
        } catch (Exception exception) {
            _log.error("There was an unknown exception trying to remove the element access data for data type: " + dataType);
            throw new RuntimeException(exception);
        }
        try {
            PoolDBUtils.ExecuteNonSelectQuery(query2, user.getDBName(), login);
        } catch (SQLException exception) {
            _log.error("There was a SQL exception trying to remove the element access data for data type: " + dataType);
            throw new RuntimeException(exception);
        } catch (Exception exception) {
            _log.error("There was an unknown exception trying to remove the element access data for data type: " + dataType);
            throw new RuntimeException(exception);
        }
    }

    private static final Log _log = LogFactory.getLog(DeleteXdatElementSecurity.class);
}
