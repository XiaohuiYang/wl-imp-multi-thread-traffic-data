package com.adobe.acs.imp.analytics.integration;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.imp.analytics.business.AnalyticsCampaignInfo;
import com.adobe.acs.imp.analytics.business2.CampaignTrafficDataSummary;
import com.adobe.acs.imp.analytics.service.FindCampaignsForAggregateAnalyticsService;
import com.adobe.acs.imp.core.integration.IMPJSONResponseBuilder;
import com.adobe.acs.imp.core.integration.IMPJSONResponseWriter;

@Component(metatype = true, label = "Query Related Campaign Paths For Analytics Page2",
description = "Servlet for querying related campaign paths for analytics page2")
@Service
@Property(name = "sling.servlet.paths", value = "/bin/imp/findCampaignForAggregatedTraffic", propertyPrivate = true)
public class FindCampaignsForAggregateTrafficServlet2 extends SlingAllMethodsServlet 
{
	private static final long serialVersionUID = -948422914479557233L;
	private static final String HITS_KEY_NAME = "hits";
	
	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	@Reference
	FindCampaignsForAggregateAnalyticsService findCampaignService;
	
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException
	{
		this.doPost(request, response);
	}
	
	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException
	{
		ResourceResolver resourceResolver = request.getResourceResolver();
		IMPJSONResponseBuilder responseBuilder = new IMPJSONResponseBuilder();
		IMPJSONResponseWriter responseWriter = new IMPJSONResponseWriter(response);
		
		String productReference = request.getParameter("productReference");
		String focus = request.getParameter("focus");
		String targetMarketGeography = request.getParameter("targetMarketGeography");
		String targetMarketLanguage = request.getParameter("targetMarketLanguage");
		String startDate = request.getParameter("startDate");
		String endDate = request.getParameter("endDate");

		try
		{
			List<AnalyticsCampaignInfo> campaignInfos = findCampaignService.getCampaignsForTraffic(resourceResolver, productReference, focus, targetMarketGeography, targetMarketLanguage, startDate, endDate);
			CampaignTrafficDataSummary summaryCampaignTrafficService = new CampaignTrafficDataSummary(findCampaignService);
			summaryCampaignTrafficService.addSummaryData(resourceResolver, campaignInfos, startDate, endDate);
			writeResponse(responseBuilder, campaignInfos);
		}
		catch (Exception e) 
		{
			logger.error("Unable to create JSON Object", e);
		}
		finally
		{
			if (resourceResolver != null)
			{
				resourceResolver.close();
				resourceResolver = null;
			}
	
			try
			{
				String responseBuilderStr = responseBuilder.build().toString();
				responseWriter.writeResponse(responseBuilderStr);
				logger.debug("Response for aggregate traffic campaign ids : " + responseBuilderStr);
			} catch (JSONException e)
			{
				logger.error("Unable to write response message for lock service. ", e);
			} 
		}
	}
	
	private void writeResponse(IMPJSONResponseBuilder responseBuilder, List<AnalyticsCampaignInfo> campaignInfos) throws JSONException
	{
		responseBuilder.addResponseObject(HITS_KEY_NAME, new Object[0]);
		
		for(AnalyticsCampaignInfo info : campaignInfos)
		{
			if (info.getMap() == null || info.getMap().isEmpty())
			{
				continue;
			}
			JSONObject response = new JSONObject();
			response.put("campaignId", info.getCampaignId());
			response.put("campaignName", info.getCampaignName());
			response.put("summary", info.getMap());
			responseBuilder.addResponseObject(HITS_KEY_NAME, response);
		}
	}
}
