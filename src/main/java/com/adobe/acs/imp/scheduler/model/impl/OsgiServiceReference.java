package com.adobe.acs.imp.scheduler.model.impl;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;

public class OsgiServiceReference
{
	private final Class<?> clazz;
	private BundleContext bundleContext = null;
	private ServiceReference serviceReference = null;
	private Object service = null;
	
	public <Clazz> OsgiServiceReference(Class<Clazz> clazz)
	{
		this.clazz = clazz;
	}
	
	public <Clazz> OsgiServiceReference(BundleContext bundleContext, Class<Clazz> clazz)
	{
		this.bundleContext = bundleContext;
		this.clazz = clazz;
	}
	
	@SuppressWarnings("unchecked")
	public <Clazz> Clazz getService() throws Exception
	{
		if (service != null)
			return (Clazz) service;
		
		if (bundleContext == null)
			bundleContext = BundleReference.class.cast(this.getClass().getClassLoader()).getBundle().getBundleContext();
		
		serviceReference = bundleContext.getServiceReference(clazz.getName());
		if (serviceReference == null)
			return null;
		
		service = (Clazz) bundleContext.getService(serviceReference);
		return (Clazz) service;
	}
	
	public void close()
	{
		if (serviceReference != null)
			 bundleContext.ungetService(serviceReference);
	}
}
