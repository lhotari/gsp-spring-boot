/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.boot.prototype.gsp;

import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.StandaloneTagLibraryLookup;
import org.codehaus.groovy.grails.web.pages.discovery.CachingGrailsConventionGroovyPageLocator;
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageLocator;
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

@Configuration
@ConditionalOnClass(GroovyPagesTemplateEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class GspAutoConfiguration {
    @Configuration
    @Import({TagLibraryLookupRegistrar.class})
    protected static class GspTemplateEngineAutoConfiguration {
        @Bean(autowire=Autowire.BY_NAME)
        @ConditionalOnMissingBean(name="groovyPagesTemplateEngine") 
        GroovyPagesTemplateEngine groovyPagesTemplateEngine() {
            return new GroovyPagesTemplateEngine();
        }
        
        @Bean(autowire=Autowire.BY_NAME)
        @ConditionalOnMissingBean(name="groovyPageLocator")
        GroovyPageLocator groovyPageLocator() {
            CachingGrailsConventionGroovyPageLocator pageLocator = new CachingGrailsConventionGroovyPageLocator() {
                protected List<String> resolveSearchPaths(String uri) {
                    return Arrays.asList(new String[]{"classpath:/templates" + uri, uri});
                }
            };
            pageLocator.setReloadEnabled(false);
            return pageLocator;
        }
        
        @Bean(autowire=Autowire.BY_NAME)
        @ConditionalOnMissingBean(name = "gspViewResolver")
        public GrailsViewResolver gspViewResolver() {
            GrailsViewResolver gspViewResolver = new GrailsViewResolver();
            gspViewResolver.setAllowGrailsViewCaching(true);
            return gspViewResolver;
        }
    }
    
    protected static class TagLibraryLookupRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            if(!registry.containsBeanDefinition("gspTagLibraryLookup")) {
                GenericBeanDefinition beanDefinition = createBeanDefinition(StandaloneTagLibraryLookup.class);
                
                ManagedList<BeanDefinition> list = new ManagedList<BeanDefinition>();
                registerTagLibs(list);
                
                beanDefinition.getPropertyValues().addPropertyValue("tagLibInstances", list);
                
                registry.registerBeanDefinition("gspTagLibraryLookup", beanDefinition);
                registry.registerAlias("gspTagLibraryLookup", "tagLibraryLookup");
            }
        }

        protected void registerTagLibs(ManagedList<BeanDefinition> list) {
            for(Class<?> taglibClazz : StandaloneTagLibraryLookup.DEFAULT_TAGLIB_CLASSES) {
                list.add(createBeanDefinition(taglibClazz));
            }
        }

        protected GenericBeanDefinition createBeanDefinition(Class<?> beanClass) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(beanClass);
            beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME);
            return beanDefinition;
        }
    }
}
