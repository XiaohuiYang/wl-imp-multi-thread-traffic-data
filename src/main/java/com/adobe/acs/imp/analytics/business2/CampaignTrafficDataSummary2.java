package com.adobe.acs.imp.analytics.business2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;

import com.adobe.acs.imp.analytics.business.AnalyticsCampaignInfo;
import com.adobe.acs.imp.analytics.dao.CampaignAnalyticsDAO;
import com.adobe.acs.imp.analytics.service.FindCampaignsForAggregateAnalyticsService;
import com.adobe.acs.imp.cache.CacheManager;
import com.adobe.acs.imp.cache.data.CachedCampaignInfo;
import com.adobe.acs.imp.cache.data.CachedTrafficData;
import com.adobe.acs.imp.cache.IMPCache.Entry;
import com.adobe.acs.imp.core.constants.AnalyticsJcrConstants;
import com.adobe.acs.imp.core.infrastructure.ImpLoggerFactory;

public class CampaignTrafficDataSummary2 {
	
	private FindCampaignsForAggregateAnalyticsService findCampaignService;
	private CacheManager cacheManager;
	private final Logger logger = ImpLoggerFactory.getLogger(CampaignTrafficDataSummary.class);
	
	private StringBuilder resultSB = new StringBuilder();

	public CampaignTrafficDataSummary2(
			CacheManager cacheManager, FindCampaignsForAggregateAnalyticsService findCampaignService) {
		this.findCampaignService = findCampaignService;
		this.cacheManager = cacheManager;
	}

	public String addSummaryData(ResourceResolver resourceResolver, List<AnalyticsCampaignInfo> campaignInfos, String startDate,
			        String endDate, String productReference, String focus, String targetMarketGeography, String targetMarketLanguage) throws JSONException, RepositoryException 
	{
		logger.debug("Begin of campaigns data retriving.");
		if (campaignInfos == null) {
			getAllFromCache(startDate, endDate, productReference, focus, targetMarketGeography, targetMarketLanguage);
		}
		else {
			for (AnalyticsCampaignInfo info : campaignInfos) {
				CachedCampaignInfo cachedData = null;
				if (cacheManager != null) {
					cachedData = cacheManager.get(info.getCampaignId());
				}
				if (!summaryTrafficDataFromCache(cachedData, info, startDate, endDate)) {
					logger.debug("Campaign Data is missing in cache : " + info.getCampaignId());
					JSONObject json = findCampaignService.getTrafficDataForCampaigns(resourceResolver, info.getCampaignId(), info.getCampaignName(), startDate, endDate);
					summaryTraffic(info,json);
					builderJson(info);
				}
				if (resultSB.length() > 0 && resultSB.charAt(resultSB.length() -1) != ',') {
					resultSB.append(",");
				}
			}
		}
		logger.debug("End of campaigns data retriving.");
		return resultSB.length() > 0 ? resultSB.substring(0, resultSB.length() -1) : "";
	}

	private void getAllFromCache(String startDate, String endDate,
			String productReference, String focus, String targetMarketGeography, String targetMarketLanguage) {
		Iterator<Entry<String, CachedCampaignInfo>> iter = cacheManager.getCacheIterator();
		while (iter.hasNext()) {
			Entry<String, CachedCampaignInfo> item = iter.next();
			if (!validate(productReference, focus,  targetMarketGeography, targetMarketLanguage)) {
				continue;
			}
			summaryTrafficDataFromCache(item.getValue(), null, startDate, endDate);
		}
		
	}

	private boolean validate(String productReference, String focus,
			String targetMarketGeography, String targetMarketLanguage) {
		// To be Done;
		return false;
	}

	private void builderJson(AnalyticsCampaignInfo info) {
		if (info.getMap() == null || info.getMap().size() ==0)
			return;
		resultSB.append( "{");
		resultSB.append( "\"campaignId\":\"" + info.getCampaignId() + "\",");
		resultSB.append( "\"campaignName\":\"" + info.getCampaignName() + "\",");
		resultSB.append( "\"summary\":");
		buildInfoDataString(info);
		resultSB.append( "}");
	}

	private void buildInfoDataString(AnalyticsCampaignInfo info) {
		resultSB.append( "{" );
		resultSB.append( "\""+ AnalyticsJcrConstants.TRAFFIC_IMPRESSIONS +"\":" + info.getMap().get( AnalyticsJcrConstants.TRAFFIC_IMPRESSIONS) + ",");
		resultSB.append( "\""+ AnalyticsJcrConstants.TRAFFIC_REACH +"\":" + info.getMap().get( AnalyticsJcrConstants.TRAFFIC_REACH) + ",");
		resultSB.append( "\""+ AnalyticsJcrConstants.TRAFFIC_USERS +"\":" + info.getMap().get( AnalyticsJcrConstants.TRAFFIC_USERS) + ",");
		resultSB.append( "\""+ AnalyticsJcrConstants.TRAFFIC_CLICKS +"\":" + info.getMap().get( AnalyticsJcrConstants.TRAFFIC_CLICKS) + ",");
		resultSB.append( "\""+ AnalyticsJcrConstants.TRAFFIC_CTR +"\":" + info.getMap().get( AnalyticsJcrConstants.TRAFFIC_CTR));
		resultSB.append( "}");
	}

	private boolean summaryTrafficDataFromCache(CachedCampaignInfo cachedData,
			AnalyticsCampaignInfo info, String startDate, String endDate) {
		if (cachedData == null) {
			if ( cacheManager != null) {
				if (cacheManager.isCachedAll()) {
					return true;
				}
			}
		}
			//return (cacheManager != null && cacheManager.isCachedAll());
		
		int impressions = 0;
		double reach = 0.0;
		int users = 0;
		int clicks = 0;
		
		
		int lowDate = Integer.parseInt(startDate.replaceAll("-", ""));
		int highDate = Integer.parseInt(endDate.replaceAll("-", ""));
		boolean hasData = false;
		
		for (CachedTrafficData dailyData:cachedData.getTrafficCampaignReport()) {
			if (dailyData.getDate() >= lowDate && dailyData.getDate() <= highDate) {
				hasData =  true;
				clicks += dailyData.getClicks();
				impressions += dailyData.getImpressions();
				users +=  dailyData.getUsers();
				reach += dailyData.getReach();
			}
		}
		if (!hasData) {
			return true;
		}
		double ctr = ((impressions == 0) ? 0.0 : clicks / impressions);
		
		resultSB.append("{");
		resultSB.append("\"campaignId\":\"" + info.getCampaignId() + "\",");
		resultSB.append("\"campaignName\":\"" + info.getCampaignName() + "\",");
		resultSB.append("\"summary\":");
		buildSummaryString(impressions, reach, users, clicks, ctr);
		resultSB.append("}");
		return true;
	}

	
	private void buildSummaryString(int impressions, double reach, int users,
			int clicks, double ctr) {
		resultSB.append("{" );
		resultSB.append("\""+ AnalyticsJcrConstants.TRAFFIC_IMPRESSIONS +"\":" + impressions + ",");
		resultSB.append("\""+ AnalyticsJcrConstants.TRAFFIC_REACH +"\":" + reach + ",");
		resultSB.append("\""+ AnalyticsJcrConstants.TRAFFIC_USERS +"\":" + users + ",");
		resultSB.append("\""+ AnalyticsJcrConstants.TRAFFIC_CLICKS +"\":" + clicks + ",");
		resultSB.append("\""+ AnalyticsJcrConstants.TRAFFIC_CTR +"\":" + ctr);
		resultSB.append("}");
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
		logger.debug("Get Data from Cache.");
		double impressions = summaryDouble(json, AnalyticsJcrConstants.TRAFFIC_IMPRESSIONS);
		double reach = summaryDouble(json, AnalyticsJcrConstants.TRAFFIC_REACH);
		double users = summaryDouble(json, AnalyticsJcrConstants.TRAFFIC_USERS);
		double clicks = summaryDouble(json, AnalyticsJcrConstants.TRAFFIC_CLICKS);
		
		double ctr = clicks / impressions;
		if (impressions < 0.0001) {
			ctr = 0.0;
		}else {
			ctr = clicks / impressions;
		}

		
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
