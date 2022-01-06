package com.liferay.samples.fbo.component.replacer;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ModelListener;

import java.util.Arrays;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
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
			
			ServiceTracker<ModelListener, ModelListener> targetModelListenerServiceTracker = new ServiceTracker<ModelListener, ModelListener>(targetBundle.getBundleContext(), ModelListener.class, null);
			targetModelListenerServiceTracker.open();
			
			long count = Arrays.asList(targetModelListenerServiceTracker.getServiceReferences()).stream().filter(
					reference -> reference != null 
					&& reference.getProperty("component.name") != null 
					&& reference.getProperty("component.name")
					.equals(TARGET_COMPONENT)).count();

			if(count == 0) {
				
				_log.debug("Adding bundle listener to bundle " + currentBundle.getSymbolicName());
				context.addBundleListener(new CurrentBundleStartedListener());
				
			} else {
			
				_log.debug("Adding service listener to bundle " + targetBundle.getSymbolicName());
				this.targetComponentUnregisteringListener = new TargetComponentUnregisteringListener(currentBundle, targetBundle);
				this.targetBundle = targetBundle;
				targetBundle.getBundleContext().addServiceListener(this.targetComponentUnregisteringListener);
				
			}
		});
		
	}

	private void enableComponent(BundleContext context) {
		ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime> serviceComponentRuntimeServiceTracker = new ServiceTracker<ServiceComponentRuntime, ServiceComponentRuntime>(context, ServiceComponentRuntime.class, null);
		serviceComponentRuntimeServiceTracker.open();
		ServiceComponentRuntime scr = serviceComponentRuntimeServiceTracker.getService();
		
		ComponentDescriptionDTO dto = scr.getComponentDescriptionDTO(context.getBundle(), NEW_COMPONENT);
		scr.enableComponent(dto);
		
		serviceComponentRuntimeServiceTracker.close();
	}
	
	private TargetComponentUnregisteringListener targetComponentUnregisteringListener;
	private CurrentBundleStartedListener currentBundleStartedListener;
	
	private Bundle targetBundle;

	@Override
	public void stop(BundleContext context) throws Exception {

		if(this.targetComponentUnregisteringListener != null && !this.targetComponentUnregisteringListener.isTerminated()) {
			targetBundle.getBundleContext().removeServiceListener(this.targetComponentUnregisteringListener);
		}

		if(this.currentBundleStartedListener != null && !this.currentBundleStartedListener.isTerminated()) {
			context.removeBundleListener(this.currentBundleStartedListener);
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

				_log.debug("Service unregistering " + event.getServiceReference().getProperty("component.name"));

				enableComponent(this.currentBundle.getBundleContext());
				
				this.targetBundle.getBundleContext().removeServiceListener(this);
				this.terminated = true;
				
			}
			
		}
		
		public boolean isTerminated() {
			return this.terminated;
		}
		
	}
	
	private class CurrentBundleStartedListener implements BundleListener {

		private boolean terminated = false;
		
		@Override
		public void bundleChanged(BundleEvent event) {

			if(event.getType() == BundleEvent.STARTED) {
				
				_log.debug("Bundle started " + event.getBundle().getSymbolicName());
				enableComponent(event.getBundle().getBundleContext());

				event.getBundle().getBundleContext().removeBundleListener(this);
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
