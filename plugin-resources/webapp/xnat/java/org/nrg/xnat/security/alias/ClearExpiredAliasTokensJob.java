/*
 * org.nrg.xnat.security.alias.ClearExpiredAliasTokensJob
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security.alias;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.schedule.JobInterface;
import org.nrg.xdat.XDAT;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class ClearExpiredAliasTokensJob implements JobInterface {
    /**
     * Initializes the job bean. All parameters must be passed in here on bean creation.
     *
     * @param context    The execution context.
     * @throws org.quartz.JobExecutionException
     *          When something goes wrong with job execution.
     */
    @Override
    public void init(final JobExecutionContext context) throws JobExecutionException {
        _timeout = (String) context.getMergedJobDataMap().get("tokenTimeout");
        _queries = (List<String>) context.getMergedJobDataMap().get("queries");
        if (_log.isDebugEnabled()) {
            _log.debug("Initializing the alias token sweeper job with an interval of: " + _timeout);
        }
    }

    /**
     * Executes the job bean.
     *
     * @throws org.quartz.JobExecutionException
     *          When something goes wrong with job execution.
     */
    @Override
    public void execute() throws JobExecutionException {
        if (_log.isDebugEnabled()) {
            _log.debug("Executing alias token sweep function");
        }
        JdbcTemplate template = new JdbcTemplate(XDAT.getDataSource());
        if (_queries == null) {
            throw new JobExecutionException("No queries defined for the expired alias tokens job: you must provide queries to clear expired tokens");
        }
        for (String format : _queries) {
            final String query = String.format(format, _timeout);
            if (_log.isDebugEnabled()) {
                _log.debug("Executing alias token sweep query: " + query);
            }
            template.execute(query);
        }
    }

    /**
     * Destroys the job bean. This can be used for task clean-up, etc.
     */
    @Override
    public void destroy() {
        // Nothing to do to destroy the bean here...
        if (_log.isDebugEnabled()) {
            _log.debug("Destroying the alias token sweeper job");
        }
    }

    private static final Log _log = LogFactory.getLog(ClearExpiredAliasTokensJob.class);
    private String _timeout;
    private List<String> _queries;
}
