package net.sf.ecl1.utilities.logging;

import org.eclipse.core.runtime.Plugin;

import net.sf.ecl1.utilities.Activator;

/**
 * A factory class for creating loggers that work in Eclipse and non-Eclipse (standalone) environments.
 * <p>
 * The factory determines the correct logger type based on the environment:
 * <ul>
 *     <li>If running in Eclipse, it returns an instance of {@link ConsoleLogger}</li>
 *     <li>If running in a non-Eclipse environment, it returns an instance of {@link StandaloneLogger}</li>
 * </ul>
 */
public class LoggerFactory {

    /**
     * Create a new {@link ConsoleLogger} or {@link StandaloneLogger} based on the environment.
     * <p>
     * For {@link StandaloneLogger} pluginId & errorLogLogger can be null.
     * 
     * @param className class that wants to log
     * @param pluginId the identifier of the plugin that wants to log or null (standalone)
     * @param activatorPlugin Plugin instance (logs to Eclipse's ErrorLog view) or null (standalone)
     * 
     */
    public static ICommonLogger getLogger(String className, String pluginId, Plugin activatorPlugin) {
        if (Activator.isRunningInEclipse()) {
            if (activatorPlugin == null || pluginId == null) {
                throw new IllegalArgumentException("Eclipse environment requires a Plugin instance and pluginId");
            }
            return new ConsoleLogger(activatorPlugin.getLog(), pluginId, className);
        } else {
            return new StandaloneLogger(className);
        }
    }
}
