/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.weaving.AspectJWeavingEnabler;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Parser for the &lt;context:load-time-weaver/&gt; element.
 * 用于解析 <context:load-time-weaver/> 节点的解析器
 * @author Juergen Hoeller
 * @since 2.5
 */
class LoadTimeWeaverBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	/**
	 * The bean name of the internally managed AspectJ weaving enabler.
	 * @since 4.3.1
	 */
	public static final String ASPECTJ_WEAVING_ENABLER_BEAN_NAME =
			"org.springframework.context.config.internalAspectJWeavingEnabler";

	private static final String ASPECTJ_WEAVING_ENABLER_CLASS_NAME =
			"org.springframework.context.weaving.AspectJWeavingEnabler";

	private static final String DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME =
			"org.springframework.context.weaving.DefaultContextLoadTimeWeaver";

	private static final String WEAVER_CLASS_ATTRIBUTE = "weaver-class";

	private static final String ASPECTJ_WEAVING_ATTRIBUTE = "aspectj-weaving";

	//spring会为配置<context:load-time-weaver/>封装为一个bean，
	//且该bean的className为org.springframework.context.weaving.DefaultContextLoadTimeWeaver，即完成了对DefaultContextLoadTimeWeaver的注册
	@Override
	protected String getBeanClassName(Element element) {
		if (element.hasAttribute(WEAVER_CLASS_ATTRIBUTE)) {
			return element.getAttribute(WEAVER_CLASS_ATTRIBUTE);
		}
		return DEFAULT_LOAD_TIME_WEAVER_CLASS_NAME;
	}

	//且该bean的id为loadTimeWeaver
	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		return ConfigurableApplicationContext.LOAD_TIME_WEAVER_BEAN_NAME;
	}

	/*LoadTimeWeaverBeanDefinitionParser（BeanDefinitionParser的间接实现类，且核心逻辑从parse()开始）经过父类
	* AbstractSingleBeanDefinitionParser封装，其核心逻辑被转移到doParse()中*/
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		//设置BeanDefinition的角色
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		/*<context:load-time-weaver/>节点
		* 若显示设置aspectj-weaving属性，直接根据设置的值（on|off）对应返回（true|false）；
		* 若未显示设置aspectj-weaving属性，则使用默认的情况（autodetect），则检测META-INF/aop.xml资源文件是否存在*/
		/*是否开启AspectJ功能，因为全局配置中配置<context:load-time-weaver/>仅仅表示加入该功能，但并不意味着该功能能使用，
		* 还需通过aspectj-weaving进行配置，aspectj-weaving可选on|off|autodetect（默认值）,即默认情况下，若只配置了<context:load-time-weaver/>
		* spring会去检测资源文件META-INF/aop.xml是否存在，若存在则表示可以使用AspectJ功能*/
		if (isAspectJWeavingEnabled(element.getAttribute(ASPECTJ_WEAVING_ATTRIBUTE), parserContext)) {
			if (!parserContext.getRegistry().containsBeanDefinition(ASPECTJ_WEAVING_ENABLER_BEAN_NAME)) {
				//将org.springframework.context.weaving.AspectJWeavingEnabler封装在BeanDefinition中进行注册
				RootBeanDefinition def = new RootBeanDefinition(ASPECTJ_WEAVING_ENABLER_CLASS_NAME);
				parserContext.registerBeanComponent(
						new BeanComponentDefinition(def, ASPECTJ_WEAVING_ENABLER_BEAN_NAME));
			}
			if (isBeanConfigurerAspectEnabled(parserContext.getReaderContext().getBeanClassLoader())) {
				new SpringConfiguredBeanDefinitionParser().parse(element, parserContext);
			}
		}
	}

	protected boolean isAspectJWeavingEnabled(String value, ParserContext parserContext) {
		if ("on".equals(value)) {
			return true;
		}
		else if ("off".equals(value)) {
			return false;
		}
		else {
			// Determine default...
			//自动检测
			ClassLoader cl = parserContext.getReaderContext().getBeanClassLoader();
			return (cl != null && cl.getResource(AspectJWeavingEnabler.ASPECTJ_AOP_XML_RESOURCE) != null);
		}
	}

	protected boolean isBeanConfigurerAspectEnabled(@Nullable ClassLoader beanClassLoader) {
		return ClassUtils.isPresent(SpringConfiguredBeanDefinitionParser.BEAN_CONFIGURER_ASPECT_CLASS_NAME,
				beanClassLoader);
	}

}
