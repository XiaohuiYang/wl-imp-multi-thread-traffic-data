package com.adobe.acs.imp.analytics.integration;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.imp.analytics.business.AnalyticsCampaignInfo;
import com.adobe.acs.imp.analytics.business2.CampaignTrafficDataSummary2;
import com.adobe.acs.imp.analytics.service.FindCampaignsForAggregateAnalyticsService;
import com.adobe.acs.imp.cache.CacheManager;

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
	
	@Reference
	CacheManager cacheManager;
	
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException
	{
		this.doPost(request, response);
	}
	
	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException
	{
		ResourceResolver resourceResolver = request.getResourceResolver();
		
		String productReference = request.getParameter("productReference");
		String focus = request.getParameter("focus");
		String targetMarketGeography = request.getParameter("targetMarketGeography");
		String targetMarketLanguage = request.getParameter("targetMarketLanguage");
		String startDate = request.getParameter("startDate");
		String endDate = request.getParameter("endDate");
		String responseBuilderStr = "";

		try
		{
			List<AnalyticsCampaignInfo> campaignInfos = null;
			if (cacheManager == null || !cacheManager.isCachedAll() ) {
				campaignInfos = findCampaignService.getCampaignsForTraffic(resourceResolver, productReference, focus, targetMarketGeography, targetMarketLanguage, startDate, endDate);
				if (campaignInfos == null) {
					campaignInfos = new ArrayList<AnalyticsCampaignInfo>();
				}
			}
			CampaignTrafficDataSummary2 summaryCampaignTrafficService = new CampaignTrafficDataSummary2(cacheManager, findCampaignService);
			responseBuilderStr = summaryCampaignTrafficService.addSummaryData(resourceResolver, campaignInfos, startDate, endDate, 
					 productReference, focus, targetMarketGeography, targetMarketLanguage);
			responseBuilderStr = "{\"" + HITS_KEY_NAME + "\":[" + responseBuilderStr + "]}";
			JSONObject json = new JSONObject(responseBuilderStr);
			responseBuilderStr = json.toString();
			logger.debug("before write ");
			//responseWriter.writeResponse(responseBuilderStr);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().print(responseBuilderStr);
			logger.debug("after write ");
		}
		catch (Throwable e) 
		{
			logger.error("", e);
		}
		finally
		{
			logger.debug("Response for aggregate traffic campaign ids : " + responseBuilderStr);
			if (resourceResolver != null)
			{
				resourceResolver.close();
				resourceResolver = null;
			}
		}
	}

//	private String gzip(String json, SlingHttpServletResponse response) {
//		try {
//			ByteArrayOutputStream out = new  ByteArrayOutputStream();
//			GZIPOutputStream zipper = new GZIPOutputStream(out);
//			zipper.write(json.getBytes());
//			zipper.close();
//			response.setHeader("Content-Encoding", "gzip");
//			return out.toString();
//		} catch (IOException e) {
//			return json;
//		}
//	}
}
