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

import java.util.List;
import java.util.Map;

import org.codehaus.groovy.grails.commons.DefaultGrailsTagLibClass;
import org.codehaus.groovy.grails.commons.GrailsTagLibClass;
import org.codehaus.groovy.grails.plugins.web.taglib.RenderTagLib;
import org.codehaus.groovy.grails.plugins.web.taglib.SitemeshTagLib;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.TagLibraryLookup;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import taglib.FormatTagLib;

@Configuration
@ConditionalOnClass(GroovyPagesTemplateEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class GspAutoConfiguration {
    @Configuration
    @Import({GroovyPagesTemplateEngineRegistrar.class, TagLibraryLookupRegistrar.class})
    protected static class GspTemplateEngineAutoConfiguration {
        
    }
    
    protected static class GroovyPagesTemplateEngineRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            if(!registry.containsBeanDefinition("groovyPagesTemplateEngine")) {
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(GroovyPagesTemplateEngine.class);
                beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME);
                registry.registerBeanDefinition("groovyPagesTemplateEngine", beanDefinition);
            }
        }
    }
    
    protected static class TagLibraryLookupRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            if(!registry.containsBeanDefinition("gspTagLibraryLookup")) {
                GenericBeanDefinition beanDefinition = createBeanDefinition(SpringBootTagLibraryLookup.class);
                
                ManagedList<BeanDefinition> list = new ManagedList<BeanDefinition>();
                registerTagLibs(list);
                
                beanDefinition.getPropertyValues().addPropertyValue("tagLibInstances", list);
                
                registry.registerBeanDefinition("gspTagLibraryLookup", beanDefinition);
                registry.registerAlias("gspTagLibraryLookup", "tagLibraryLookup");
            }
        }

        protected void registerTagLibs(ManagedList<BeanDefinition> list) {
            list.add(createBeanDefinition(SitemeshTagLib.class));
            list.add(createBeanDefinition(RenderTagLib.class));
            list.add(createBeanDefinition(FormatTagLib.class));
        }

        protected GenericBeanDefinition createBeanDefinition(Class<?> beanClass) {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(beanClass);
            beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_NAME);
            return beanDefinition;
        }
    }

    private static final class SpringBootTagLibraryLookup extends TagLibraryLookup {
        List<Object> tagLibInstances; 
        
        private SpringBootTagLibraryLookup() {
            
        }

        public void afterPropertiesSet() {
            registerTagLibraries();
            registerTemplateNamespace();
        }

        protected void registerTagLibraries() {
            for(Object tagLibInstance : tagLibInstances) {
                registerTagLib(new DefaultGrailsTagLibClass(tagLibInstance.getClass()));
            }
        }

        @Override
        protected void putTagLib(Map<String, Object> tags, String name, GrailsTagLibClass taglib) {
            for(Object tagLibInstance : tagLibInstances) {
                if(tagLibInstance.getClass() == taglib.getClazz()) {
                    tags.put(name, tagLibInstance);
                    break;
                }
            }
        }

        public List<Object> getTagLibInstances() {
            return tagLibInstances;
        }

        public void setTagLibInstances(List<Object> tagLibInstances) {
            this.tagLibInstances = tagLibInstances;
        }
    }
    
    @Configuration
    @AutoConfigureAfter(GspTemplateEngineAutoConfiguration.class)
    protected static class GspViewResolverAutoConfiguration {
        @Autowired
        private ResourceLoader resourceLoader = new DefaultResourceLoader();

        @Bean
        @ConditionalOnMissingBean(name = "gspViewResolver")
        public GspViewResolver gspViewResolver(GroovyPagesTemplateEngine groovyPagesTemplateEngine) {
            return new GspViewResolver(groovyPagesTemplateEngine, this.resourceLoader);
        }
    }    
}
