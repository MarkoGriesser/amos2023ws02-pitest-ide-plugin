// SPDX-FileCopyrightText: 2023 Lennart Heimbs
//
// SPDX-License-Identifier: MIT

package io.github.amosproj.pitmutationmate.override.strategy

import io.github.amosproj.pitmutationmate.override.PITSettingOverridePlugin
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * DependencyInclusionStrategy
 *
 * This class is responsible for adding a dependency to the pitest plugin extension.
 * It is used by the [PITSettingOverridePlugin].
 *
 * @see [ PITSettingOverridePlugin ]
 */
class DependencyInclusionStrategy implements OverrideStrategy {

    public static final String OVERRIDE_ATTRIBUTE = 'addCoverageListenerDependency'
    static final String PITEST_EXTENSION = 'pitest'
    static final String PITEST_PLUGIN = 'info.solidsoft.pitest'
    static final String PITEST_PLUGIN_ANDROID = 'pl.droidsonroids.pitest'

    @SuppressWarnings('FieldName')
    private final static Logger log = Logging.getLogger(PITSettingOverridePlugin)

    @Override
    void apply(Project proj, String propertyName, String overrideValue) {
        log.debug("Trying to add dependency '$overrideValue'.")

        boolean isApplied = false
        proj.allprojects {
            Project project = it
            def pitestPlugin = getPlugin(project, PITEST_PLUGIN)
            def androidPitestPlugin = getPlugin(project, PITEST_PLUGIN_ANDROID)

            if (pitestPlugin != null) {
                log.debug("Adding dependency for detected Pitest Plugin $PITEST_PLUGIN")
                def pitestConfig = project.configurations.findByName(PITEST_EXTENSION)
                if (pitestConfig != null) {
                    project.dependencies.add(PITEST_EXTENSION, overrideValue)
                    isApplied = true
                    log.info("Successfully added dependency '$overrideValue' to project ${project.name}.")
                } else {
                    log.warn("Configuration '$PITEST_EXTENSION' does not exist in project ${project.name}. Skipping dependency addition.")
                }
            } else if (androidPitestPlugin != null) {
                // For Android, check buildscript configurations
                def pitestConfig = project.buildscript.configurations.findByName(PITEST_EXTENSION)
                if (pitestConfig != null) {
                    project.buildscript.dependencies.add(PITEST_EXTENSION, overrideValue)
                    isApplied = true
                    log.info("Successfully added dependency '$overrideValue' to buildscript of project ${project.name}.")
                } else {
                    log.warn("Configuration '$PITEST_EXTENSION' does not exist in buildscript of project ${project.name}. Skipping dependency addition.")
                }
            } else {
                log.info('No Pitest Plugin detected')
                return
            }
        }
        if (!isApplied) {
            throw new GradleException('PITest plugin not found or configuration missing. Please apply the PITest plugin first.')
        }
    }

    /**
     * Find a plugin in the project or its subprojects.
     * @param project : The project to search in.
     * @param pluginId : The id of the plugin to search for.
     * @return The plugin if found, null otherwise.
     */
    private static Object getPlugin(Project project, String pluginId) {
        def plugin = project.plugins.findPlugin(pluginId)
        def projectIterator = project.subprojects.iterator()
        def subproject
        while (plugin == null && projectIterator.hasNext()) {
            subproject = projectIterator.next()
            plugin = subproject.plugins.findPlugin(pluginId)
        }
        return plugin
    }

}
