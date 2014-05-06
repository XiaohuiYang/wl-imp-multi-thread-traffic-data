package com.adobe.acs.imp.analytics.business2;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;

import com.adobe.acs.imp.analytics.business.AnalyticsCampaignInfo;
import com.adobe.acs.imp.analytics.dao.CampaignAnalyticsDAO;
import com.adobe.acs.imp.analytics.service.FindCampaignsForAggregateAnalyticsService;
import com.adobe.acs.imp.core.constants.AnalyticsJcrConstants;
import com.adobe.acs.imp.core.infrastructure.ImpLoggerFactory;
import com.adobe.acs.imp.scheduler.model.impl.OsgiServiceReference;

public class SummaryTrafficDataForSingleCampaignTask implements Runnable {
	
	private FindCampaignsForAggregateAnalyticsService _findCampaignService;
	private AnalyticsCampaignInfo _info;
	private String _startDate;
	private String _endDate;
	
	private final Logger logger = ImpLoggerFactory.getLogger(SummaryTrafficDataForSingleCampaignTask.class);
	
	public SummaryTrafficDataForSingleCampaignTask(FindCampaignsForAggregateAnalyticsService findCampaignService, AnalyticsCampaignInfo info, 
			String startDate, String endDate) {
		_findCampaignService = findCampaignService;
		_info = info;
		_startDate = startDate;
		_endDate= endDate;
	}

	public void run() {
		ResourceResolver resovler = null;
		try {
			OsgiServiceReference ref = new OsgiServiceReference(ResourceResolverFactory.class);
			ResourceResolverFactory factory = (ResourceResolverFactory) ref.getService();
			resovler = factory.getAdministrativeResourceResolver(null);
			JSONObject json = _findCampaignService.getTrafficDataForCampaigns(resovler, _info.getCampaignId(), _info.getCampaignName(), _startDate, _endDate);
			summaryTraffic(_info, json);
		}catch (Exception e) {
			logger.debug("Error when summary the campaigns' traffic data: " + _info.getCampaignId(), e);
		}finally {
			if (resovler != null) {
				resovler.close();
			}
		}

		
	}
	
	private void summaryTraffic(AnalyticsCampaignInfo info, JSONObject json) 
	{
		JSONArray hits = null ;
		try {
			hits = json.getJSONArray(CampaignAnalyticsDAO.HITS_KEY_NAME);
		} catch (JSONException e) {
			logger.error("Unable to get JSON Object");
		}
		if (hits == null || hits.length() == 0)
		{
			return;
		}
		double impressions = summaryDouble(json, AnalyticsJcrConstants.TRAFFIC_IMPRESSIONS);
		double reach = summaryDouble(json, AnalyticsJcrConstants.TRAFFIC_REACH);
		double users = summaryDouble(json, AnalyticsJcrConstants.TRAFFIC_USERS);
		double clicks = summaryDouble(json, AnalyticsJcrConstants.TRAFFIC_CLICKS);
		double ctr = clicks / impressions;
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(AnalyticsJcrConstants.TRAFFIC_IMPRESSIONS, new Double(impressions));
		map.put(AnalyticsJcrConstants.TRAFFIC_REACH, new Double(reach));
		map.put(AnalyticsJcrConstants.TRAFFIC_USERS, new Double(users));
		map.put(AnalyticsJcrConstants.TRAFFIC_CLICKS, new Double(clicks));
		map.put(AnalyticsJcrConstants.TRAFFIC_CTR, new Double(ctr));
		
		info.setMap(map);
	}
	
	private double summaryDouble(JSONObject json, String field) {
		double result = 0.0;
		JSONArray hits;
		try {
			hits = json.getJSONArray(CampaignAnalyticsDAO.HITS_KEY_NAME);
			for (int i=0; i < hits.length(); i++) 
			{
				JSONObject analyticJson = hits.getJSONObject(i);
				result += analyticJson.getDouble(field);
			}
		} catch (JSONException e) 
		{
			logger.error("Unable to get JSON Object");
		}
		return result;
	}
}
