package com.adobe.acs.imp.analytics.business2;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.slf4j.Logger;

import com.adobe.acs.imp.analytics.business.AnalyticsCampaignInfo;
import com.adobe.acs.imp.analytics.service.FindCampaignsForAggregateAnalyticsService;
import com.adobe.acs.imp.core.infrastructure.ImpLoggerFactory;

public class CampaignTrafficDataSummary {
	
	private static final int THRESHOLD_THREADS = 50;
	private static final int THEADPOOL_SIZE  =  20;
	private FindCampaignsForAggregateAnalyticsService findCampaignService;
	private final Logger logger = ImpLoggerFactory.getLogger(CampaignTrafficDataSummary.class);

	public CampaignTrafficDataSummary(
			FindCampaignsForAggregateAnalyticsService findCampaignService) {
		this.findCampaignService = findCampaignService;
	}

	public void addSummaryData(ResourceResolver resourceResolver, List<AnalyticsCampaignInfo> campaignInfos, String startDate,
			        String endDate) throws JSONException, RepositoryException 
	{
		logger.debug("Begin of campaigns data retriving.");
		if (campaignInfos == null)
			return;
		if (campaignInfos.size() >= THRESHOLD_THREADS) {
			logger.debug("Using Thread Pool to Get Summary the campaigns' traffic data.");
			addSummaryDataWithPool(campaignInfos, startDate, endDate);
			logger.debug("End of campaigns data retriving.");
			
			return;
		}
		for (AnalyticsCampaignInfo info : campaignInfos) {
			Runnable task =  new SummaryTrafficDataForSingleCampaignTask(findCampaignService, info, startDate, endDate);
			task.run();
		}
		logger.debug("End of campaigns data retriving.");
	}


	private void addSummaryDataWithPool(List<AnalyticsCampaignInfo> campaignInfos, String startDate,
			String endDate) {
		ExecutorService pool = Executors.newFixedThreadPool(THEADPOOL_SIZE); 
		for (AnalyticsCampaignInfo info : campaignInfos) {
			Runnable task =  new SummaryTrafficDataForSingleCampaignTask(findCampaignService, info, startDate, endDate);
			pool.execute(task);
		}
		pool.shutdown();
		try {
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			logger.debug("Error in ThreadPool,",e);
		}
		
	}




}
