/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.framework;

import org.springframework.aop.SpringProxy;

import java.io.Serializable;
import java.lang.reflect.Proxy;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 * @since 12.03.2004
 */
@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

	/**
	 * 默认是采用JDK静态代理，对beanClass为非接口实现类采取CGLIB动态代理
	 *
	 * @author YaoS
	 * @date 2020/3/6 16:40
	 */
	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		/* spring中的代理分为JDK代理和Cglib代理
		 * optimize:用来控制通过cglib创建的代理是否适用激进的优化策略。目前该属性仅用于cglib代理，对JDK动态代理(缺省代理)无效
		 * proxyTargetClass:为true时，目标类本身被代理而不是目标类的接口被代理。若设为true，CGLIB代理将被创建，设置方式为
		 * <aop:aspectj-autoproxy proxy-target-class="true">
		 * hasNoUserSuppliedProxyInterfaces:用于判断是否存在代理接口
		 * JDK动态代理和CGLIB字节码生成的区别
		 * JDK动态代理只能对实现了接口的类生成代理，而不能针对类
		 * CGLIB是针对类实现代理，主要是对指定的类生成一个子类，覆盖其中的方法，因为是继承，故该类或方法不应声明为final类型*/
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			/*若目标类对象实现了接口(即是接口类型)，默认情况下会采用JDK的动态代理实现AOP*/
			/*若目标类对象没有实现接口，必须采用CGLIB库，Spring会自动在JDK动态代理和CGLIB之间转换*/
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				//创建JDK动态代理
				return new JdkDynamicAopProxy(config);
			}
			/*若目标类对象实现了接口，可强制使用CGLIB实现AOP
			 * 实现方式：1.添加CGLIB库，Spring_HOME/cglib/*.jar
			 * 2.在配置文件中配置<aop:aspectj-autoproxy proxy-target-class="true">*/
			//创建Cglib代理
			return new ObjenesisCglibAopProxy(config);
		} else {
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
