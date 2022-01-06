package com.liferay.samples.fbo.component.replacer;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.Arrays;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.tracker.ServiceTracker;

public class CustomBundleActivator implements BundleActivator {

	private final static String TARGET_BUNDLE = "com.liferay.sharing.service";
	
	private final static String TARGET_COMPONENT = "com.liferay.sharing.internal.model.listener.UserModelListener";

	private final static String NEW_COMPONENT = "com.liferay.samples.fbo.component.replacer.UserModelListener";

	@Override
	public void start(BundleContext context) throws Exception {
		
		Bundle currentBundle = context.getBundle();

		Bundle[] bundles = context.getBundles();
		Arrays.asList(bundles).stream().filter(bundle -> bundle.getSymbolicName().equals(TARGET_BUNDLE)).findFirst().ifPresent(targetBundle -> {
			_log.debug("Adding service listener to bundle " + targetBundle.getSymbolicName());
			this.listener = new TargetComponentUnregisteringListener(currentBundle, targetBundle);
			this.targetBundle = targetBundle;
			targetBundle.getBundleContext().addServiceListener(this.listener);
		});
		
	}
	
	private TargetComponentUnregisteringListener listener;
	private Bundle targetBundle;

	@Override
	public void stop(BundleContext context) throws Exception {

		if(!this.listener.isTerminated()) {
			targetBundle.getBundleContext().removeServiceListener(this.listener);
		}

	}

	private class TargetComponentUnregisteringListener implements ServiceListener {

		private Bundle currentBundle;
		private Bundle targetBundle;
		private boolean terminated = false;
		
		public TargetComponentUnregisteringListener(Bundle currentBundle, Bundle targetBundle) {
			this.currentBundle = currentBundle;
			this.targetBundle = targetBundle;
		}
		
		@Override
		public void serviceChanged(ServiceEvent event) {

			if(event.getServiceReference() != null
					&& event.getServiceReference().getProperty("component.name") != null
					&& event.getServiceReference().getProperty("component.name").equals(TARGET_COMPONENT) 
					&& event.getType() == ServiceEvent.UNREGISTERING) {

				ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> serviceComponentRuntimeServiceTracker = new ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime>(this.currentBundle.getBundleContext(), ServiceComponentRuntime.class, null);
				serviceComponentRuntimeServiceTracker.open();
				ServiceComponentRuntime scr = serviceComponentRuntimeServiceTracker.getService();
				
				_log.debug("Service unregistering " + event.getServiceReference().getProperty("component.name"));

				ComponentDescriptionDTO dto = scr.getComponentDescriptionDTO(this.currentBundle, NEW_COMPONENT);
				scr.enableComponent(dto);
				
				serviceComponentRuntimeServiceTracker.close();
				
				this.targetBundle.getBundleContext().removeServiceListener(this);
				this.terminated = true;
				
			}
			
		}
		
		public boolean isTerminated() {
			return this.terminated;
		}
		
	}
	
	private static final Log _log = LogFactoryUtil.getLog(
			CustomBundleActivator.class);
	
}
