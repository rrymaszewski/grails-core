/* Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.resolve.config

import org.codehaus.groovy.grails.resolve.EnhancedDefaultDependencyDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId

abstract class AbstractDependenciesConfigurer extends AbstractDependencyManagementConfigurer {

    private static final DEPENDENCY_PATTERN = ~/([a-zA-Z0-9\-\/\._+=]*?):([a-zA-Z0-9\-\/\._+=]+?):([a-zA-Z0-9\-\/\._+=]+)/
    
    private assistant = new Assistant()
    
    AbstractDependenciesConfigurer(DependencyConfigurationContext context) {
        super(context)
    }
    
    def invokeMethod(String name, args) {
        if (!args) {
            println "WARNING: Configurational method [$name] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring.."
        } else if (assistant.isOnlyStrings(args)) {
            assistant.addDependencyStrings(name, args.toList(), null, null)
        } else if (assistant.isProperties(args)) {
            assistant.addDependencyMaps(name, args.toList(), null)
        } else if (assistant.isStringsAndConfigurer(args)) {
            assistant.addDependencyStrings(name, args[0..-2], null, args[-1])
        } else if (assistant.isPropertiesAndConfigurer(args)) {
            assistant.addDependencyMaps(name, args[0..-2], args[-1])
        } else if (assistant.isStringsAndProperties(args)) {
            assistant.addDependencyStrings(name, args[0..-2], args[-1], null)
        } else {
            println "WARNING: Configurational method [$name] in grails-app/conf/BuildConfig.groovy doesn't exist. Ignoring.."
        }
    }

    // Private class to get the methods out of scope for closures
    // using the enclosing class as a delegate.
    private class Assistant {
        private isOnlyStrings(args) {
            args.every { it instanceof CharSequence }
        }

        private isStringsAndConfigurer(args) {
            if (args.size() == 1) {
                false
            } else {
                isOnlyStrings(args[0..-2]) && args[-1] instanceof Closure
            }
        }

        private isStringsAndProperties(args) {
            if (args.size() == 1) {
                false
            } else {
                isOnlyStrings(args[0..-2]) && args[-1] instanceof Map
            }
        }

        private isProperties(args) {
            args.every { it instanceof Map }
        }

        private isPropertiesAndConfigurer(args) {
            if (args.size() == 1) {
                false
            } else {
                isProperties(args[0..-2]) && args[-1] instanceof Closure
            }
        }

        private Map extractDependencyProperties(String scope, String dependency) {
            def matcher = dependency =~ AbstractDependenciesConfigurer.DEPENDENCY_PATTERN
            if (matcher.matches()) {
                def properties = [:]
                properties.name = matcher[0][2]
                properties.group = matcher[0][1]
                properties.version = matcher[0][3]
                properties
            } else {
                println "WARNING: Specified dependency definition ${scope}(${dependency}) is invalid! Skipping.."
                null
            }
        }

        private addDependencyStrings(String scope, Collection<String> dependencies, Map overrides, Closure configurer) {
            for (dependency in dependencies) {
                def dependencyProperties = extractDependencyProperties(scope, dependency)
                if (!dependencyProperties) {
                    continue
                }

                if (overrides) {
                    dependencyProperties.putAll(overrides)
                }

                addDependency(scope, dependencyProperties, configurer)
            }
        }

        protected addDependencyMaps(String scope, Collection<Map> dependencies, Closure configurer) {
            for (dependency in dependencies) {
                addDependency(scope, dependency, configurer)
            }
        }

        private addDependency(String scope, Map dependency, Closure configurer) {
            def context = AbstractDependenciesConfigurer.this.context
            def dependencyManager = AbstractDependenciesConfigurer.this.dependencyManager
            
            preprocessDependencyProperties(dependency)

            def isExcluded = context.pluginName ? dependencyManager.isExcludedFromPlugin(context.pluginName, dependency.name) : dependencyManager.isExcluded(dependency.name)
            if (isExcluded) {
                return
            }

            def attrs = [:]
            if (dependency.classifier) {
                attrs["m:classifier"] = dependency.classifier
            }

            def mrid
            if (dependency.branch) {
                mrid = ModuleRevisionId.newInstance(dependency.group, dependency.name, dependency.branch, dependency.version, attrs)
            } else {
                mrid = ModuleRevisionId.newInstance(dependency.group, dependency.name, dependency.version, attrs)
            }

            def dependencyDescriptor = new EnhancedDefaultDependencyDescriptor(mrid, false, getBooleanValueOrDefault(dependency, 'transitive', true), scope)

            dependencyDescriptor.exported = getBooleanValueOrDefault(dependency, 'export', true)
            dependencyDescriptor.inherited = context.inherited || dependencyManager.inheritsAll || context.pluginName

            if (context.pluginName) {
                dependencyDescriptor.plugin = context.pluginName
            }

            if (configurer) {
                dependencyDescriptor.configure(configurer)
            }

            addDependency(scope, dependencyDescriptor)
        }
        
        private boolean getBooleanValueOrDefault(Map properties, String propertyName, boolean defaultValue) {
            properties.containsKey(propertyName) ? Boolean.valueOf(properties[propertyName]) : defaultValue
        }
    }
    
    protected preprocessDependencyProperties(Map dependency) {
        // used in plugin subclass to populate default group id
    }
    
    abstract protected addDependency(String scope, EnhancedDefaultDependencyDescriptor descriptor)
    
}