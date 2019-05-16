package net.sf.ecl1.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ui.IStartup;

import net.sf.ecl1.utilities.general.ConsoleLogger;

/**
 * This class registers HisRuntimeClasspathProvider as the classpath provider for JUnit launch configurations.
 * 
 * @author TNeumann
 */
public class HisRuntimeClasspathProviderManager implements IStartup, ILaunchConfigurationListener {
    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID, HisRuntimeClasspathProviderManager.class.getSimpleName());
    
	@Override
	public void earlyStartup() {
		// register HisRuntimeClasspathProvider for existing JUnit launch configurations
		logger.info("HisRuntimeClasspathProviderManager is registering HisRuntimeClasspathProvider as classpath provider for JUnit tests...");
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfiguration[] launchConfigs = null;
		try {
			launchConfigs = manager.getLaunchConfigurations();
		} catch (CoreException e) {
			logger.error("HisRuntimeClasspathProviderManager failed to obtain launch configurations: " + e, e);
		}
		if (launchConfigs!=null && launchConfigs.length>0) {
			for (int i = 0; i < launchConfigs.length; i++) {
				processLaunchConfiguration(launchConfigs[i]);
			}
		}
		
		// now register as ILaunchConfigurationListener to be able to register HisRuntimeClasspathProvider in new launch configurations, too
		manager.addLaunchConfigurationListener(this);
	}

	@Override
	public void launchConfigurationAdded(ILaunchConfiguration launchConfig) {
		logger.info("New launch configuration " + launchConfig);
		processLaunchConfiguration(launchConfig);
	}

	@Override
	public void launchConfigurationChanged(ILaunchConfiguration launchConfig) {
		// ignore
	}

	@Override
	public void launchConfigurationRemoved(ILaunchConfiguration launchConfig) {
		// ignore
	}
	
	private void processLaunchConfiguration(ILaunchConfiguration launchConfig) {
		logger.debug("Launch config: " + launchConfig);
		try {
			ILaunchConfigurationType launchConfigType = launchConfig.getType();
			String launchConfigTypeName = launchConfigType!=null ? launchConfigType.getName() : null;
			logger.debug("Launch config type name = " + launchConfigTypeName);
			if (launchConfigType.getName().equals("JUnit")) {
				// set new classpath provider for JUnit tests
				ILaunchConfigurationWorkingCopy wc = launchConfig.getWorkingCopy();
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, HisRuntimeClasspathProvider.CLASSPATH_PROVIDER_EXTENSION_ID);
				wc.doSave();
				logger.info("Registered HisRuntimeClasspathProvider as classpath provider for launch configuration " + launchConfig);
			}
		} catch (CoreException e) {
			logger.error("HisRuntimeClasspathProviderManager failed to register classpath provider for launch config " + launchConfig + ": " + e, e);
		}
	}
}
