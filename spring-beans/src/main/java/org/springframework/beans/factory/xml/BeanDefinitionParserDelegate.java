/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanMetadataAttribute;
import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.parsing.*;
import org.springframework.beans.factory.support.*;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * Stateful delegate class used to parse XML bean definitions.
 * Intended for use by both the main parser and any extension
 * {@link BeanDefinitionParser BeanDefinitionParsers} or
 * {@link BeanDefinitionDecorator BeanDefinitionDecorators}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Fisher
 * @author Gary Russell
 * @see ParserContext
 * @see DefaultBeanDefinitionDocumentReader
 * @since 2.0
 */
public class BeanDefinitionParserDelegate {

	public static final String BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans";

	public static final String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; ";

	/**
	 * Value of a T/F attribute that represents true.
	 * Anything else represents false. Case seNsItive.
	 */
	public static final String TRUE_VALUE = "true";

	public static final String FALSE_VALUE = "false";

	public static final String DEFAULT_VALUE = "default";

	public static final String DESCRIPTION_ELEMENT = "description";

	public static final String AUTOWIRE_NO_VALUE = "no";

	public static final String AUTOWIRE_BY_NAME_VALUE = "byName";

	public static final String AUTOWIRE_BY_TYPE_VALUE = "byType";

	public static final String AUTOWIRE_CONSTRUCTOR_VALUE = "constructor";

	public static final String AUTOWIRE_AUTODETECT_VALUE = "autodetect";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String BEAN_ELEMENT = "bean";

	public static final String META_ELEMENT = "meta";

	public static final String ID_ATTRIBUTE = "id";

	public static final String PARENT_ATTRIBUTE = "parent";

	public static final String CLASS_ATTRIBUTE = "class";

	public static final String ABSTRACT_ATTRIBUTE = "abstract";

	public static final String SCOPE_ATTRIBUTE = "scope";

	private static final String SINGLETON_ATTRIBUTE = "singleton";

	public static final String LAZY_INIT_ATTRIBUTE = "lazy-init";

	public static final String AUTOWIRE_ATTRIBUTE = "autowire";

	public static final String AUTOWIRE_CANDIDATE_ATTRIBUTE = "autowire-candidate";

	public static final String PRIMARY_ATTRIBUTE = "primary";

	public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";

	public static final String INIT_METHOD_ATTRIBUTE = "init-method";

	public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";

	public static final String FACTORY_METHOD_ATTRIBUTE = "factory-method";

	public static final String FACTORY_BEAN_ATTRIBUTE = "factory-bean";

	public static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";

	public static final String INDEX_ATTRIBUTE = "index";

	public static final String TYPE_ATTRIBUTE = "type";

	public static final String VALUE_TYPE_ATTRIBUTE = "value-type";

	public static final String KEY_TYPE_ATTRIBUTE = "key-type";

	public static final String PROPERTY_ELEMENT = "property";

	public static final String REF_ATTRIBUTE = "ref";

	public static final String VALUE_ATTRIBUTE = "value";

	public static final String LOOKUP_METHOD_ELEMENT = "lookup-method";

	public static final String REPLACED_METHOD_ELEMENT = "replaced-method";

	public static final String REPLACER_ATTRIBUTE = "replacer";

	public static final String ARG_TYPE_ELEMENT = "arg-type";

	public static final String ARG_TYPE_MATCH_ATTRIBUTE = "match";

	public static final String REF_ELEMENT = "ref";

	public static final String IDREF_ELEMENT = "idref";

	public static final String BEAN_REF_ATTRIBUTE = "bean";

	public static final String PARENT_REF_ATTRIBUTE = "parent";

	public static final String VALUE_ELEMENT = "value";

	public static final String NULL_ELEMENT = "null";

	public static final String ARRAY_ELEMENT = "array";

	public static final String LIST_ELEMENT = "list";

	public static final String SET_ELEMENT = "set";

	public static final String MAP_ELEMENT = "map";

	public static final String ENTRY_ELEMENT = "entry";

	public static final String KEY_ELEMENT = "key";

	public static final String KEY_ATTRIBUTE = "key";

	public static final String KEY_REF_ATTRIBUTE = "key-ref";

	public static final String VALUE_REF_ATTRIBUTE = "value-ref";

	public static final String PROPS_ELEMENT = "props";

	public static final String PROP_ELEMENT = "prop";

	public static final String MERGE_ATTRIBUTE = "merge";

	public static final String QUALIFIER_ELEMENT = "qualifier";

	public static final String QUALIFIER_ATTRIBUTE_ELEMENT = "attribute";

	public static final String DEFAULT_LAZY_INIT_ATTRIBUTE = "default-lazy-init";

	public static final String DEFAULT_MERGE_ATTRIBUTE = "default-merge";

	public static final String DEFAULT_AUTOWIRE_ATTRIBUTE = "default-autowire";

	public static final String DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE = "default-autowire-candidates";

	public static final String DEFAULT_INIT_METHOD_ATTRIBUTE = "default-init-method";

	public static final String DEFAULT_DESTROY_METHOD_ATTRIBUTE = "default-destroy-method";


	protected final Log logger = LogFactory.getLog(getClass());

	private final XmlReaderContext readerContext;

	private final DocumentDefaultsDefinition defaults = new DocumentDefaultsDefinition();

	private final ParseState parseState = new ParseState();

	/**
	 * Stores all used bean names so we can enforce uniqueness on a per
	 * beans-element basis. Duplicate bean ids/names may not exist within the
	 * same level of beans element nesting, but may be duplicated across levels.
	 */
	private final Set<String> usedNames = new HashSet<>();


	/**
	 * Create a new BeanDefinitionParserDelegate associated with the supplied
	 * {@link XmlReaderContext}.
	 */
	public BeanDefinitionParserDelegate(XmlReaderContext readerContext) {
		Assert.notNull(readerContext, "XmlReaderContext must not be null");
		this.readerContext = readerContext;
	}


	/**
	 * Get the {@link XmlReaderContext} associated with this helper instance.
	 */
	public final XmlReaderContext getReaderContext() {
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor}
	 * to pull the source metadata from the supplied {@link Element}.
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return this.readerContext.extractSource(ele);
	}

	/**
	 * Report an error with the given message for the given source element.
	 */
	protected void error(String message, Node source) {
		this.readerContext.error(message, source, this.parseState.snapshot());
	}

	/**
	 * Report an error with the given message for the given source element.
	 */
	protected void error(String message, Element source) {
		this.readerContext.error(message, source, this.parseState.snapshot());
	}

	/**
	 * Report an error with the given message for the given source element.
	 */
	protected void error(String message, Element source, Throwable cause) {
		this.readerContext.error(message, source, this.parseState.snapshot(), cause);
	}


	/**
	 * Initialize the default settings assuming a {@code null} parent delegate.
	 */
	public void initDefaults(Element root) {
		initDefaults(root, null);
	}

	/**
	 * Initialize the default lazy-init, autowire, dependency check settings,
	 * init-method, destroy-method and merge settings. Support nested 'beans'
	 * element use cases by falling back to the given parent in case the
	 * defaults are not explicitly set locally.
	 *
	 * @see #populateDefaults(DocumentDefaultsDefinition, DocumentDefaultsDefinition, org.w3c.dom.Element)
	 * @see #getDefaults()
	 */
	public void initDefaults(Element root, @Nullable BeanDefinitionParserDelegate parent) {
		populateDefaults(this.defaults, (parent != null ? parent.defaults : null), root);
		this.readerContext.fireDefaultsRegistered(this.defaults);
	}

	/**
	 * Populate the given DocumentDefaultsDefinition instance with the default lazy-init,
	 * autowire, dependency check settings, init-method, destroy-method and merge settings.
	 * Support nested 'beans' element use cases by falling back to {@code parentDefaults}
	 * in case the defaults are not explicitly set locally.
	 *
	 * @param defaults       the defaults to populate
	 * @param parentDefaults the parent BeanDefinitionParserDelegate (if any) defaults to fall back to
	 * @param root           the root element of the current bean definition document (or nested beans element)
	 */
	protected void populateDefaults(DocumentDefaultsDefinition defaults, @Nullable DocumentDefaultsDefinition parentDefaults, Element root) {
		String lazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
		if (DEFAULT_VALUE.equals(lazyInit)) {
			// Potentially inherited from outer <beans> sections, otherwise falling back to false.
			lazyInit = (parentDefaults != null ? parentDefaults.getLazyInit() : FALSE_VALUE);
		}
		defaults.setLazyInit(lazyInit);

		String merge = root.getAttribute(DEFAULT_MERGE_ATTRIBUTE);
		if (DEFAULT_VALUE.equals(merge)) {
			// Potentially inherited from outer <beans> sections, otherwise falling back to false.
			merge = (parentDefaults != null ? parentDefaults.getMerge() : FALSE_VALUE);
		}
		defaults.setMerge(merge);

		String autowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE);
		if (DEFAULT_VALUE.equals(autowire)) {
			// Potentially inherited from outer <beans> sections, otherwise falling back to 'no'.
			autowire = (parentDefaults != null ? parentDefaults.getAutowire() : AUTOWIRE_NO_VALUE);
		}
		defaults.setAutowire(autowire);

		if (root.hasAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE)) {
			defaults.setAutowireCandidates(root.getAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE));
		} else if (parentDefaults != null) {
			defaults.setAutowireCandidates(parentDefaults.getAutowireCandidates());
		}

		if (root.hasAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE)) {
			defaults.setInitMethod(root.getAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE));
		} else if (parentDefaults != null) {
			defaults.setInitMethod(parentDefaults.getInitMethod());
		}

		if (root.hasAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE)) {
			defaults.setDestroyMethod(root.getAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE));
		} else if (parentDefaults != null) {
			defaults.setDestroyMethod(parentDefaults.getDestroyMethod());
		}

		defaults.setSource(this.readerContext.extractSource(root));
	}

	/**
	 * Return the defaults definition object.
	 */
	public DocumentDefaultsDefinition getDefaults() {
		return this.defaults;
	}

	/**
	 * Return the default settings for bean definitions as indicated within
	 * the attributes of the top-level {@code <beans/>} element.
	 */
	public BeanDefinitionDefaults getBeanDefinitionDefaults() {
		BeanDefinitionDefaults bdd = new BeanDefinitionDefaults();
		bdd.setLazyInit("TRUE".equalsIgnoreCase(this.defaults.getLazyInit()));
		bdd.setAutowireMode(getAutowireMode(DEFAULT_VALUE));
		bdd.setInitMethodName(this.defaults.getInitMethod());
		bdd.setDestroyMethodName(this.defaults.getDestroyMethod());
		return bdd;
	}

	/**
	 * Return any patterns provided in the 'default-autowire-candidates'
	 * attribute of the top-level {@code <beans/>} element.
	 */
	@Nullable
	public String[] getAutowireCandidatePatterns() {
		String candidatePattern = this.defaults.getAutowireCandidates();
		return (candidatePattern != null ? StringUtils.commaDelimitedListToStringArray(candidatePattern) : null);
	}


	/**
	 * Parses the supplied {@code <bean>} element. May return {@code null}
	 * if there were errors during parse. Errors are reported to the
	 * {@link org.springframework.beans.factory.parsing.ProblemReporter}.
	 */
	@Nullable
	public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
		return parseBeanDefinitionElement(ele, null);
	}

	/**
	 * Parses the supplied {@code <bean>} element. May return {@code null}
	 * if there were errors during parse. Errors are reported to the
	 * {@link org.springframework.beans.factory.parsing.ProblemReporter}.
	 */
	@Nullable
	public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
		//解析id属性
		String id = ele.getAttribute(ID_ATTRIBUTE);
		//解析name属性
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		//分割name属性
		List<String> aliases = new ArrayList<>();
		if (StringUtils.hasLength(nameAttr)) {
			String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
			aliases.addAll(Arrays.asList(nameArr));
		}

		String beanName = id;
		if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
			beanName = aliases.remove(0);
			if (logger.isTraceEnabled()) {
				logger.trace("No XML 'id' specified - using '" + beanName +
						"' as bean name and " + aliases + " as aliases");
			}
		}

		if (containingBean == null) {
			checkNameUniqueness(beanName, aliases, ele);
		}
		//进一步解析其他所有属性并统一封装到GenericBeanDefinition类型的实例中
		AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
		if (beanDefinition != null) {
			if (!StringUtils.hasText(beanName)) {
				try {
					//若不存在beanName，则根据Spring提供的命名规则为当前的bean生成beanName
					if (containingBean != null) {
						beanName = BeanDefinitionReaderUtils.generateBeanName(
								beanDefinition, this.readerContext.getRegistry(), true);
					} else {
						beanName = this.readerContext.generateBeanName(beanDefinition);
						// Register an alias for the plain bean class name, if still possible,
						// if the generator returned the class name plus a suffix.
						// This is expected for Spring 1.2/2.0 backwards compatibility.
						String beanClassName = beanDefinition.getBeanClassName();
						if (beanClassName != null &&
								beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
								!this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
							aliases.add(beanClassName);
						}
					}
					if (logger.isTraceEnabled()) {
						logger.trace("Neither XML 'id' nor 'name' specified - " +
								"using generated bean name [" + beanName + "]");
					}
				} catch (Exception ex) {
					error(ex.getMessage(), ele);
					return null;
				}
			}
			String[] aliasesArray = StringUtils.toStringArray(aliases);
			//将获取到的信息封装到BeanDefinitionHolder类型的实例中
			return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
		}

		return null;
	}

	/**
	 * Validate that the specified bean name and aliases have not been used already
	 * within the current level of beans element nesting.
	 */
	protected void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
		String foundName = null;

		if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
			foundName = beanName;
		}
		if (foundName == null) {
			foundName = CollectionUtils.findFirstMatch(this.usedNames, aliases);
		}
		if (foundName != null) {
			error("Bean name '" + foundName + "' is already used in this <beans> element", beanElement);
		}

		this.usedNames.add(beanName);
		this.usedNames.addAll(aliases);
	}

	/**
	 * Parse the bean definition itself, without regard to name or aliases. May return
	 * {@code null} if problems occurred during the parsing of the bean definition.
	 */
	@Nullable
	public AbstractBeanDefinition parseBeanDefinitionElement(
			Element ele, String beanName, @Nullable BeanDefinition containingBean) {

		this.parseState.push(new BeanEntry(beanName));
		//解析class属性
		String className = null;
		if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
			className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
		}
		//解析parent属性
		String parent = null;
		if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
			parent = ele.getAttribute(PARENT_ATTRIBUTE);
		}

		try {
			//创建用于封装bean节点所有属性AbstractBeanDefinition类型的GenericBeanDefinition实例
			AbstractBeanDefinition bd = createBeanDefinition(className, parent);
			//解析给定的bean节点的所有属性并封装到bean * definition中
			parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
			//提取description
			bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));
			//解析当前bean节点下的所有meta子节点
			parseMetaElements(ele, bd);
			/**
			 * 解析lookup-method和replace-method子节点都是
			 * 1.先构造MethodOverrides实例methodOverrides
			 * 2.然后保存在methodOverrides的Set<MethodOverrides>中
			 * 3.最终记录在AbstractBeanDefinition的methodOverrides属性中
			 */
			//解析当前bean节点下所有的lookup-method子节点
			parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
			//解析当前bean节点下所有的replace-method子节点
			parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
			//解析当前bean节点下所有的constructor-arg子节点
			parseConstructorArgElements(ele, bd);
			//解析当前bean节点下所有的property子节点
			parsePropertyElements(ele, bd);
			//解析当前bean节点下所有的qualifier子节点
			parseQualifierElements(ele, bd);

			bd.setResource(this.readerContext.getResource());
			bd.setSource(extractSource(ele));

			return bd;
		} catch (ClassNotFoundException ex) {
			error("Bean class [" + className + "] not found", ele, ex);
		} catch (NoClassDefFoundError err) {
			error("Class that bean class [" + className + "] depends on not found", ele, err);
		} catch (Throwable ex) {
			error("Unexpected failure during bean definition parsing", ele, ex);
		} finally {
			this.parseState.pop();
		}

		return null;
	}

	/**
	 * Apply the attributes of the given bean element to the given bean * definition.
	 *
	 * @param ele            bean declaration element
	 * @param beanName       bean name
	 * @param containingBean containing bean definition
	 * @return a bean definition initialized according to the bean element attributes
	 */
	public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName,
																@Nullable BeanDefinition containingBean, AbstractBeanDefinition bd) {
		//
		if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
			error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
		}
		//解析scope属性
		else if (ele.hasAttribute(SCOPE_ATTRIBUTE)) {
			bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
		} else if (containingBean != null) {
			// Take default from containing bean in case of an inner bean definition.
			//在嵌套bean节点得情况下，若没有单独指定scope属性，则默认使用父类的属性
			bd.setScope(containingBean.getScope());
		}
		//解析abstract属性
		if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
			bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
		}
		//解析lazy-init属性
		String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
		if (DEFAULT_VALUE.equals(lazyInit)) {
			lazyInit = this.defaults.getLazyInit();
		}
		//lazy-init属性若没有设置或者设置为其他字符，则都会被设置为false
		bd.setLazyInit(TRUE_VALUE.equals(lazyInit));
		//解析autowire属性
		String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
		bd.setAutowireMode(getAutowireMode(autowire));
		//解析depent-on属性
		if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
			String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
			bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
		}
		//解析autowire-candidate属性
		String autowireCandidate = ele.getAttribute(AUTOWIRE_CANDIDATE_ATTRIBUTE);
		if ("".equals(autowireCandidate) || DEFAULT_VALUE.equals(autowireCandidate)) {
			String candidatePattern = this.defaults.getAutowireCandidates();
			if (candidatePattern != null) {
				String[] patterns = StringUtils.commaDelimitedListToStringArray(candidatePattern);
				bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
			}
		} else {
			bd.setAutowireCandidate(TRUE_VALUE.equals(autowireCandidate));
		}
		//解析primary属性
		if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) {
			bd.setPrimary(TRUE_VALUE.equals(ele.getAttribute(PRIMARY_ATTRIBUTE)));
		}
		//解析init-method属性
		if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
			String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
			bd.setInitMethodName(initMethodName);
		} else if (this.defaults.getInitMethod() != null) {
			bd.setInitMethodName(this.defaults.getInitMethod());
			bd.setEnforceInitMethod(false);
		}
		//解析destory-method属性
		if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
			String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
			bd.setDestroyMethodName(destroyMethodName);
		} else if (this.defaults.getDestroyMethod() != null) {
			bd.setDestroyMethodName(this.defaults.getDestroyMethod());
			bd.setEnforceDestroyMethod(false);
		}
		//解析factory-method属性
		if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
			bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
		}
		//解析factory-bean属性
		if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
			bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
		}

		return bd;
	}

	/**
	 * Create a bean definition for the given class name and parent name.
	 *
	 * @param className  the name of the bean class
	 * @param parentName the name of the bean's parent bean
	 * @return the newly created bean definition
	 * @throws ClassNotFoundException if bean class resolution was attempted but failed
	 */
	protected AbstractBeanDefinition createBeanDefinition(@Nullable String className, @Nullable String parentName)
			throws ClassNotFoundException {

		return BeanDefinitionReaderUtils.createBeanDefinition(
				parentName, className, this.readerContext.getBeanClassLoader());
	}

	public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
		//获取当前节点的所有子节点
		NodeList nl = ele.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//提取meta
			if (isCandidateElement(node) && nodeNameEquals(node, META_ELEMENT)) {
				Element metaElement = (Element) node;
				String key = metaElement.getAttribute(KEY_ATTRIBUTE);
				String value = metaElement.getAttribute(VALUE_ATTRIBUTE);
				//用key、value构造BeanMetadataAttribute实例
				BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
				attribute.setSource(extractSource(metaElement));
				//记录信息
				attributeAccessor.addMetadataAttribute(attribute);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public int getAutowireMode(String attValue) {
		String att = attValue;
		if (DEFAULT_VALUE.equals(att)) {
			att = this.defaults.getAutowire();
		}
		int autowire = AbstractBeanDefinition.AUTOWIRE_NO;
		if (AUTOWIRE_BY_NAME_VALUE.equals(att)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_BY_NAME;
		} else if (AUTOWIRE_BY_TYPE_VALUE.equals(att)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
		} else if (AUTOWIRE_CONSTRUCTOR_VALUE.equals(att)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;
		} else if (AUTOWIRE_AUTODETECT_VALUE.equals(att)) {
			autowire = AbstractBeanDefinition.AUTOWIRE_AUTODETECT;
		}
		// Else leave default value.
		return autowire;
	}

	/**
	 * Parse constructor-arg sub-elements of the given bean element.
	 */
	public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//提取所有的constructor-arg子节点
			if (isCandidateElement(node) && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
				//解析constructor-arg节点
				parseConstructorArgElement((Element) node, bd);
			}
		}
	}

	/**
	 * Parse property sub-elements of the given bean element.
	 */
	public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//若为<property>节点
			if (isCandidateElement(node) && nodeNameEquals(node, PROPERTY_ELEMENT)) {
				//解析<property>节点
				parsePropertyElement((Element) node, bd);
			}
		}
	}

	/**
	 * Parse qualifier sub-elements of the given bean element.
	 */
	public void parseQualifierElements(Element beanEle, AbstractBeanDefinition bd) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//获取<qualifier>节点
			if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ELEMENT)) {
				//解析<qualifier>节点
				parseQualifierElement((Element) node, bd);
			}
		}
	}

	/**
	 * Parse lookup-override sub-elements of the given bean element.
	 */
	public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//仅当在spring默认bean的子元素下且为<lookup-method>时有效
			if (isCandidateElement(node) && nodeNameEquals(node, LOOKUP_METHOD_ELEMENT)) {
				Element ele = (Element) node;
				//获取当前bean所对应的类中要被修饰的方法(一般为抽象方法或接口方法)
				String methodName = ele.getAttribute(NAME_ATTRIBUTE);
				//获取lookup-method节点bean属性对应要返回的bean实例
				String beanRef = ele.getAttribute(BEAN_ELEMENT);
				LookupOverride override = new LookupOverride(methodName, beanRef);
				override.setSource(extractSource(ele));
				overrides.addOverride(override);
			}
		}
	}

	/**
	 * Parse replaced-method sub-elements of the given bean element.
	 */
	public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
		NodeList nl = beanEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//仅当在spring默认bean的子元素下且为<replaced-method>时有效
			if (isCandidateElement(node) && nodeNameEquals(node, REPLACED_METHOD_ELEMENT)) {
				Element replacedMethodEle = (Element) node;
				//提取name属性表示的要替换的旧的方法
				String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
				//提取replacer属性表示的用于替换旧方法的新方法
				String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
				ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
				// Look for arg-type match elements.
				List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, ARG_TYPE_ELEMENT);
				for (Element argTypeEle : argTypeEles) {
					String match = argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE);
					match = (StringUtils.hasText(match) ? match : DomUtils.getTextValue(argTypeEle));
					if (StringUtils.hasText(match)) {
						replaceOverride.addTypeIdentifier(match);
					}
				}
				replaceOverride.setSource(extractSource(replacedMethodEle));
				overrides.addOverride(replaceOverride);
			}
		}
	}

	/**
	 * Parse a constructor-arg element.
	 */
	public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
		//提取index属性
		String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
		//提取type属性
		String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
		//提取name属性
		String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
		//若显示设置了索引，执行这里
		if (StringUtils.hasLength(indexAttr)) {
			try {
				int index = Integer.parseInt(indexAttr);
				if (index < 0) { //所设置的索引值不能小于0，且取值从0开始
					error("'index' cannot be lower than 0", ele);
				} else {
					try {
						this.parseState.push(new ConstructorArgumentEntry(index));
						//解析constructor-arg节点对应的属性元素
						Object value = parsePropertyValue(ele, bd, null);
						//将解析出来的值封装到ConstructorArgumentValues.ValueHolder类型的实例中
						ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
						//将constructor-arg节点的type和name属性一并封装到valueHolder中
						if (StringUtils.hasLength(typeAttr)) {
							valueHolder.setType(typeAttr);
						}
						if (StringUtils.hasLength(nameAttr)) {
							valueHolder.setName(nameAttr);
						}
						valueHolder.setSource(extractSource(ele));
						//不允许对相同的索引重复设置参数
						if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
							error("Ambiguous constructor-arg entries for index " + index, ele);
						} else {
							/** 添加至当前BeanDefinition的constructorValues的map类型的indexedArgumentValue中
							 * 1.先获取在BeanDefinition中声明的ConstructorArgumentValues类型的constructorArgumentValues实例
							 * 2.在map集合中按照给定的索引顺序依次添加参数值
							 */
							bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
						}
					} finally {
						this.parseState.pop();
					}
				}
			} catch (NumberFormatException ex) {
				error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
			}
		}
		//若没有显示设置索引，执行这里
		else {
			try {
				this.parseState.push(new ConstructorArgumentEntry());
				Object value = parsePropertyValue(ele, bd, null);
				ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
				if (StringUtils.hasLength(typeAttr)) {
					valueHolder.setType(typeAttr);
				}
				if (StringUtils.hasLength(nameAttr)) {
					valueHolder.setName(nameAttr);
				}
				valueHolder.setSource(extractSource(ele));
				/** 添加至当前BeanDefinition的constructorValues的list类型的genericArgumentValue中
				 * 1.先获取在BeanDefinition中声明的ConstructorArgumentValues类型的constructorArgumentValues实例
				 * 2.在list集合中依次添加参数值
				 */
				bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
			} finally {
				this.parseState.pop();
			}
		}
	}

	/**
	 * Parse a property element.
	 */
	public void parsePropertyElement(Element ele, BeanDefinition bd) {
		//解析name属性
		String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
		if (!StringUtils.hasLength(propertyName)) {
			//property节点必须拥有name属性
			error("Tag 'property' must have a 'name' attribute", ele);
			return;
		}
		this.parseState.push(new PropertyEntry(propertyName));
		try {
			if (bd.getPropertyValues().contains(propertyName)) {
				//同一属性不允许拥有多个<property>定义
				error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
				return;
			}
			//获取属性对应的属性值
			//属性--属性值 参数--参数值 两者的定义其实是相同的
			Object val = parsePropertyValue(ele, bd, propertyName);
			//使用PropertyValue进行封装
			PropertyValue pv = new PropertyValue(propertyName, val);
			parseMetaElements(ele, pv);
			pv.setSource(extractSource(ele));
			//并封装在BeanDefinition内MutablePropertyValues类型实例propertyValues的List<PropertyValue>中
			bd.getPropertyValues().addPropertyValue(pv);
		} finally {
			this.parseState.pop();
		}
	}

	/**
	 * Parse a qualifier element.
	 */
	public void parseQualifierElement(Element ele, AbstractBeanDefinition bd) {
		//解析type属性
		String typeName = ele.getAttribute(TYPE_ATTRIBUTE);
		if (!StringUtils.hasLength(typeName)) {
			//<qualifier>必须要拥有一个type属性
			error("Tag 'qualifier' must have a 'type' attribute", ele);
			return;
		}
		this.parseState.push(new QualifierEntry(typeName));
		try {
			//使用AutowireCandidateQualifier进行封装
			AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(typeName);
			qualifier.setSource(extractSource(ele));
			//解析value属性
			String value = ele.getAttribute(VALUE_ATTRIBUTE);
			if (StringUtils.hasLength(value)) {
				qualifier.setAttribute(AutowireCandidateQualifier.VALUE_KEY, value);
			}
			//获取并遍历<qualifier>节点的所有子节点
			NodeList nl = ele.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				//如果是<qualifier>节点下的<attribute>子节点
				if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ATTRIBUTE_ELEMENT)) {
					Element attributeEle = (Element) node;
					//解析key属性
					String attributeName = attributeEle.getAttribute(KEY_ATTRIBUTE);
					//解析value属性
					String attributeValue = attributeEle.getAttribute(VALUE_ATTRIBUTE);
					if (StringUtils.hasLength(attributeName) && StringUtils.hasLength(attributeValue)) {
						//并使用BeanMetadataAttribute进行封装
						BeanMetadataAttribute attribute = new BeanMetadataAttribute(attributeName, attributeValue);
						attribute.setSource(extractSource(attributeEle));
						qualifier.addMetadataAttribute(attribute);
					} else {
						error("Qualifier 'attribute' tag must have a 'name' and 'value'", attributeEle);
						return;
					}
				}
			}
			bd.addQualifier(qualifier);
		} finally {
			this.parseState.pop();
		}
	}

	/**
	 * Get the value of a property element. May be a list etc.
	 * Also used for constructor arguments, "propertyName" being null in this case.
	 */
	@Nullable
	public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
		String elementName = (propertyName != null ?
				"<property> element for property '" + propertyName + "'" :
				"<constructor-arg> element");

		// Should only have one child element: ref, value, list, etc.
		//构造函数的每个参数应该只对应一种参数类型：ref类型、一般类型value、集合list类型...
		//获取constructor-arg节点的所有子节点
		NodeList nl = ele.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			//若子节点为<description>或者<meta>则直接跳过，不进行处理
			if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
					!nodeNameEquals(node, META_ELEMENT)) {
				// Child element is what we're looking for.
				//这里我们寻找的是<constructor-arg>节点的子节点，且子节点的数目只能有1个
				if (subElement != null) {
					error(elementName + " must not contain more than one sub-element", ele);
				} else {
					subElement = (Element) node;
				}
			}
		}
		//<constructor-arg>是否有ref属性
		boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
		//<constructor-arg>是否有value属性
		boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
		/**
		 * 在<constructor-arg>上及内部不允许存在的情况：
		 *   1.同时拥有ref属性和value属性
		 *   2.存在ref属性或者value属性，且同时存在子节点
		 */
		if ((hasRefAttribute && hasValueAttribute) ||
				((hasRefAttribute || hasValueAttribute) && subElement != null)) {
			error(elementName +
					" is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
		}
		//若拥有ref属性
		if (hasRefAttribute) {
			//获取ref属性的属性值，并使用RuntimeBeanReference进行封装
			String refName = ele.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(refName)) {
				error(elementName + " contains empty 'ref' attribute", ele);
			}
			RuntimeBeanReference ref = new RuntimeBeanReference(refName);
			ref.setSource(extractSource(ele));
			return ref;
		}
		//若拥有value属性
		else if (hasValueAttribute) {
			//获取value属性的属性值，并使用TypeStringValue进行封装
			TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
			valueHolder.setSource(extractSource(ele));
			return valueHolder;
		} else if (subElement != null) {
			//解析<constructor-arg>节点的<value>、<ref>、<map|list|collection>子节点
			return parsePropertySubElement(subElement, bd);
		} else {
			// Neither child element nor "ref" or "value" attribute found.
			//既没有ref属性，也没有value属性，同时没有子节点则会报错
			error(elementName + " must specify a ref or value", ele);
			return null;
		}
	}

	@Nullable
	public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd) {
		return parsePropertySubElement(ele, bd, null);
	}

	/**
	 * Parse a value, ref or collection sub-element of a property or
	 * constructor-arg element.
	 *
	 * @param ele              subelement of property element; we don't know which yet
	 * @param defaultValueType the default type (class name) for any
	 *                         {@code <value>} tag that might be created
	 */
	@Nullable
	public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd, @Nullable String defaultValueType) {
		//如果当前节点不符合默认的命名空间
		if (!isDefaultNamespace(ele)) {
			return parseNestedCustomElement(ele, bd);
		}
		//如果当前节点是<bean>
		else if (nodeNameEquals(ele, BEAN_ELEMENT)) {
			BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
			if (nestedBd != null) {
				nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
			}
			return nestedBd;
		}
		//如果当前节点是<ref>
		else if (nodeNameEquals(ele, REF_ELEMENT)) {
			// A generic reference to any name of any bean.
			//解析bean属性
			String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
			boolean toParent = false;
			if (!StringUtils.hasLength(refName)) {
				// A reference to the id of another bean in a parent context.
				//解析parent属性
				refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
				toParent = true;
				if (!StringUtils.hasLength(refName)) {
					error("'bean' or 'parent' is required for <ref> element", ele);
					return null;
				}
			}
			if (!StringUtils.hasText(refName)) {
				error("<ref> element contains empty target attribute", ele);
				return null;
			}
			/**
			 * 使用解析获得的bean属性和parent属性封装RuntimeBeanReference
			 * 若toParent=fale，则解析获取到bean属性
			 * 若toParent=true，则解析获取到parent属性
			 */
			RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
			ref.setSource(extractSource(ele));
			return ref;
		}
		//如果当前节点是<idref>
		else if (nodeNameEquals(ele, IDREF_ELEMENT)) {
			return parseIdRefElement(ele);
		}
		//如果当前节点是<value>
		else if (nodeNameEquals(ele, VALUE_ELEMENT)) {
			return parseValueElement(ele, defaultValueType);
		}
		//如果当前节点是<null>
		else if (nodeNameEquals(ele, NULL_ELEMENT)) {
			// It's a distinguished null value. Let's wrap it in a TypedStringValue
			// object in order to preserve the source location.
			TypedStringValue nullHolder = new TypedStringValue(null);
			nullHolder.setSource(extractSource(ele));
			return nullHolder;
		}
		//如果当前节点是<array>
		else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
			return parseArrayElement(ele, bd);
		}
		//如果当前节点是<list>
		else if (nodeNameEquals(ele, LIST_ELEMENT)) {
			return parseListElement(ele, bd);
		}
		//如果当前节点是<set>
		else if (nodeNameEquals(ele, SET_ELEMENT)) {
			return parseSetElement(ele, bd);
		}
		//如果当前节点是<map>
		else if (nodeNameEquals(ele, MAP_ELEMENT)) {
			return parseMapElement(ele, bd);
		}
		//如果当前节点是<props>
		else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
			return parsePropsElement(ele);
		} else {
			//如果均不是上述节点，则报未知的属性子节点错误
			error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
			return null;
		}
	}

	/**
	 * Return a typed String value Object for the given 'idref' element.
	 */
	@Nullable
	public Object parseIdRefElement(Element ele) {
		// A generic reference to any name of any bean.
		//解析bean属性
		String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
		if (!StringUtils.hasLength(refName)) {
			error("'bean' is required for <idref> element", ele);
			return null;
		}
		if (!StringUtils.hasText(refName)) {
			error("<idref> element contains empty target attribute", ele);
			return null;
		}
		//使用RuntimeBeanNameReference进行封装
		RuntimeBeanNameReference ref = new RuntimeBeanNameReference(refName);
		ref.setSource(extractSource(ele));
		return ref;
	}

	/**
	 * Return a typed String value Object for the given value element.
	 */
	public Object parseValueElement(Element ele, @Nullable String defaultTypeName) {
		// It's a literal value.
		String value = DomUtils.getTextValue(ele);
		//解析type属性
		String specifiedTypeName = ele.getAttribute(TYPE_ATTRIBUTE);
		String typeName = specifiedTypeName;
		if (!StringUtils.hasText(typeName)) {
			typeName = defaultTypeName;
		}
		try {
			TypedStringValue typedValue = buildTypedStringValue(value, typeName);
			typedValue.setSource(extractSource(ele));
			typedValue.setSpecifiedTypeName(specifiedTypeName);
			return typedValue;
		} catch (ClassNotFoundException ex) {
			error("Type class [" + typeName + "] not found for <value> element", ele, ex);
			return value;
		}
	}

	/**
	 * Build a typed String value Object for the given raw value.
	 *
	 * @see org.springframework.beans.factory.config.TypedStringValue
	 */
	protected TypedStringValue buildTypedStringValue(String value, @Nullable String targetTypeName)
			throws ClassNotFoundException {

		ClassLoader classLoader = this.readerContext.getBeanClassLoader();
		TypedStringValue typedValue;
		if (!StringUtils.hasText(targetTypeName)) {
			typedValue = new TypedStringValue(value);
		} else if (classLoader != null) {
			Class<?> targetType = ClassUtils.forName(targetTypeName, classLoader);
			typedValue = new TypedStringValue(value, targetType);
		} else {
			typedValue = new TypedStringValue(value, targetTypeName);
		}
		return typedValue;
	}

	/**
	 * Parse an array element.
	 */
	public Object parseArrayElement(Element arrayEle, @Nullable BeanDefinition bd) {
		//解析value-type属性
		String elementType = arrayEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		NodeList nl = arrayEle.getChildNodes();
		ManagedArray target = new ManagedArray(elementType, nl.getLength());
		target.setSource(extractSource(arrayEle));
		target.setElementTypeName(elementType);
		target.setMergeEnabled(parseMergeAttribute(arrayEle));
		parseCollectionElements(nl, target, bd, elementType);
		return target;
	}

	/**
	 * Parse a list element.
	 */
	public List<Object> parseListElement(Element collectionEle, @Nullable BeanDefinition bd) {
		String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		NodeList nl = collectionEle.getChildNodes();
		ManagedList<Object> target = new ManagedList<>(nl.getLength());
		target.setSource(extractSource(collectionEle));
		target.setElementTypeName(defaultElementType);
		target.setMergeEnabled(parseMergeAttribute(collectionEle));
		parseCollectionElements(nl, target, bd, defaultElementType);
		return target;
	}

	/**
	 * Parse a set element.
	 */
	public Set<Object> parseSetElement(Element collectionEle, @Nullable BeanDefinition bd) {
		String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
		NodeList nl = collectionEle.getChildNodes();
		ManagedSet<Object> target = new ManagedSet<>(nl.getLength());
		target.setSource(extractSource(collectionEle));
		target.setElementTypeName(defaultElementType);
		target.setMergeEnabled(parseMergeAttribute(collectionEle));
		parseCollectionElements(nl, target, bd, defaultElementType);
		return target;
	}

	protected void parseCollectionElements(
			NodeList elementNodes, Collection<Object> target, @Nullable BeanDefinition bd, String defaultElementType) {

		for (int i = 0; i < elementNodes.getLength(); i++) {
			Node node = elementNodes.item(i);
			if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)) {
				target.add(parsePropertySubElement((Element) node, bd, defaultElementType));
			}
		}
	}

	/**
	 * Parse a map element.
	 */
	public Map<Object, Object> parseMapElement(Element mapEle, @Nullable BeanDefinition bd) {
		String defaultKeyType = mapEle.getAttribute(KEY_TYPE_ATTRIBUTE);
		String defaultValueType = mapEle.getAttribute(VALUE_TYPE_ATTRIBUTE);

		List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
		ManagedMap<Object, Object> map = new ManagedMap<>(entryEles.size());
		map.setSource(extractSource(mapEle));
		map.setKeyTypeName(defaultKeyType);
		map.setValueTypeName(defaultValueType);
		map.setMergeEnabled(parseMergeAttribute(mapEle));

		for (Element entryEle : entryEles) {
			// Should only have one value child element: ref, value, list, etc.
			// Optionally, there might be a key child element.
			NodeList entrySubNodes = entryEle.getChildNodes();
			Element keyEle = null;
			Element valueEle = null;
			for (int j = 0; j < entrySubNodes.getLength(); j++) {
				Node node = entrySubNodes.item(j);
				if (node instanceof Element) {
					Element candidateEle = (Element) node;
					if (nodeNameEquals(candidateEle, KEY_ELEMENT)) {
						if (keyEle != null) {
							error("<entry> element is only allowed to contain one <key> sub-element", entryEle);
						} else {
							keyEle = candidateEle;
						}
					} else {
						// Child element is what we're looking for.
						if (nodeNameEquals(candidateEle, DESCRIPTION_ELEMENT)) {
							// the element is a <description> -> ignore it
						} else if (valueEle != null) {
							error("<entry> element must not contain more than one value sub-element", entryEle);
						} else {
							valueEle = candidateEle;
						}
					}
				}
			}

			// Extract key from attribute or sub-element.
			Object key = null;
			boolean hasKeyAttribute = entryEle.hasAttribute(KEY_ATTRIBUTE);
			boolean hasKeyRefAttribute = entryEle.hasAttribute(KEY_REF_ATTRIBUTE);
			if ((hasKeyAttribute && hasKeyRefAttribute) ||
					(hasKeyAttribute || hasKeyRefAttribute) && keyEle != null) {
				error("<entry> element is only allowed to contain either " +
						"a 'key' attribute OR a 'key-ref' attribute OR a <key> sub-element", entryEle);
			}
			if (hasKeyAttribute) {
				key = buildTypedStringValueForMap(entryEle.getAttribute(KEY_ATTRIBUTE), defaultKeyType, entryEle);
			} else if (hasKeyRefAttribute) {
				String refName = entryEle.getAttribute(KEY_REF_ATTRIBUTE);
				if (!StringUtils.hasText(refName)) {
					error("<entry> element contains empty 'key-ref' attribute", entryEle);
				}
				RuntimeBeanReference ref = new RuntimeBeanReference(refName);
				ref.setSource(extractSource(entryEle));
				key = ref;
			} else if (keyEle != null) {
				key = parseKeyElement(keyEle, bd, defaultKeyType);
			} else {
				error("<entry> element must specify a key", entryEle);
			}

			// Extract value from attribute or sub-element.
			Object value = null;
			boolean hasValueAttribute = entryEle.hasAttribute(VALUE_ATTRIBUTE);
			boolean hasValueRefAttribute = entryEle.hasAttribute(VALUE_REF_ATTRIBUTE);
			boolean hasValueTypeAttribute = entryEle.hasAttribute(VALUE_TYPE_ATTRIBUTE);
			if ((hasValueAttribute && hasValueRefAttribute) ||
					(hasValueAttribute || hasValueRefAttribute) && valueEle != null) {
				error("<entry> element is only allowed to contain either " +
						"'value' attribute OR 'value-ref' attribute OR <value> sub-element", entryEle);
			}
			if ((hasValueTypeAttribute && hasValueRefAttribute) ||
					(hasValueTypeAttribute && !hasValueAttribute) ||
					(hasValueTypeAttribute && valueEle != null)) {
				error("<entry> element is only allowed to contain a 'value-type' " +
						"attribute when it has a 'value' attribute", entryEle);
			}
			if (hasValueAttribute) {
				String valueType = entryEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
				if (!StringUtils.hasText(valueType)) {
					valueType = defaultValueType;
				}
				value = buildTypedStringValueForMap(entryEle.getAttribute(VALUE_ATTRIBUTE), valueType, entryEle);
			} else if (hasValueRefAttribute) {
				String refName = entryEle.getAttribute(VALUE_REF_ATTRIBUTE);
				if (!StringUtils.hasText(refName)) {
					error("<entry> element contains empty 'value-ref' attribute", entryEle);
				}
				RuntimeBeanReference ref = new RuntimeBeanReference(refName);
				ref.setSource(extractSource(entryEle));
				value = ref;
			} else if (valueEle != null) {
				value = parsePropertySubElement(valueEle, bd, defaultValueType);
			} else {
				error("<entry> element must specify a value", entryEle);
			}

			// Add final key and value to the Map.
			map.put(key, value);
		}

		return map;
	}

	/**
	 * Build a typed String value Object for the given raw value.
	 *
	 * @see org.springframework.beans.factory.config.TypedStringValue
	 */
	protected final Object buildTypedStringValueForMap(String value, String defaultTypeName, Element entryEle) {
		try {
			TypedStringValue typedValue = buildTypedStringValue(value, defaultTypeName);
			typedValue.setSource(extractSource(entryEle));
			return typedValue;
		} catch (ClassNotFoundException ex) {
			error("Type class [" + defaultTypeName + "] not found for Map key/value type", entryEle, ex);
			return value;
		}
	}

	/**
	 * Parse a key sub-element of a map element.
	 */
	@Nullable
	protected Object parseKeyElement(Element keyEle, @Nullable BeanDefinition bd, String defaultKeyTypeName) {
		NodeList nl = keyEle.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				// Child element is what we're looking for.
				if (subElement != null) {
					error("<key> element must not contain more than one value sub-element", keyEle);
				} else {
					subElement = (Element) node;
				}
			}
		}
		if (subElement == null) {
			return null;
		}
		return parsePropertySubElement(subElement, bd, defaultKeyTypeName);
	}

	/**
	 * Parse a props element.
	 */
	public Properties parsePropsElement(Element propsEle) {
		ManagedProperties props = new ManagedProperties();
		props.setSource(extractSource(propsEle));
		props.setMergeEnabled(parseMergeAttribute(propsEle));

		List<Element> propEles = DomUtils.getChildElementsByTagName(propsEle, PROP_ELEMENT);
		for (Element propEle : propEles) {
			String key = propEle.getAttribute(KEY_ATTRIBUTE);
			// Trim the text value to avoid unwanted whitespace
			// caused by typical XML formatting.
			String value = DomUtils.getTextValue(propEle).trim();
			TypedStringValue keyHolder = new TypedStringValue(key);
			keyHolder.setSource(extractSource(propEle));
			TypedStringValue valueHolder = new TypedStringValue(value);
			valueHolder.setSource(extractSource(propEle));
			props.put(keyHolder, valueHolder);
		}

		return props;
	}

	/**
	 * Parse the merge attribute of a collection element, if any.
	 */
	public boolean parseMergeAttribute(Element collectionElement) {
		String value = collectionElement.getAttribute(MERGE_ATTRIBUTE);
		if (DEFAULT_VALUE.equals(value)) {
			value = this.defaults.getMerge();
		}
		return TRUE_VALUE.equals(value);
	}

	@Nullable
	public BeanDefinition parseCustomElement(Element ele) {
		return parseCustomElement(ele, null);
	}

	/**
	 * @param ele
	 * @param containingBd 为父类bean，对顶层元素的解析应设置为null
	 * @return
	 */
	@Nullable
	public BeanDefinition parseCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
		//获取对应的命名空间
		String namespaceUri = getNamespaceURI(ele);
		if (namespaceUri == null) {
			return null;
		}
		// 根据命名空间找到对应的NamespaceHandler
		// Aop是由AopNamespaceHandler解析
		NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
		if (handler == null) {
			error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
			return null;
		}
		//调用自定义的NamespaceHandler进行解析
		return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
	}

	public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder definitionHolder) {
		return decorateBeanDefinitionIfRequired(ele, definitionHolder, null);
	}

	/**
	 * @param ele
	 * @param definitionHolder
	 * @param containingBd     父类bean，当对某个嵌套配置进行分析时，须传递父类beanDefinition
	 *                         传递该参数是为了当子类bean没有设置scope属性时，默认使用父类bean的scope属性
	 * @return
	 */
	public BeanDefinitionHolder decorateBeanDefinitionIfRequired(
			Element ele, BeanDefinitionHolder definitionHolder, @Nullable BeanDefinition containingBd) {

		BeanDefinitionHolder finalDefinition = definitionHolder;

		// Decorate based on custom attributes first.
		//遍历所有属性，先用用户定义的属性节点装饰beanDefinition
		NamedNodeMap attributes = ele.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node node = attributes.item(i);
			finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
		}

		// Decorate based on custom nested elements.
		//遍历所有子节点，再用用户定义的嵌套标签节点装饰beanDefinition
		NodeList children = ele.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
			}
		}
		return finalDefinition;
	}

	public BeanDefinitionHolder decorateIfRequired(
			Node node, BeanDefinitionHolder originalDef, @Nullable BeanDefinition containingBd) {
		//获取当前节点的命令空间
		String namespaceUri = getNamespaceURI(node);
		//对非默认命名空间的节点进行处理
		if (namespaceUri != null && !isDefaultNamespace(namespaceUri)) {
			//根据命名空间找到对应的命名空间处理器
			NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
			if (handler != null) {
				//进行装饰处理
				BeanDefinitionHolder decorated =
						handler.decorate(node, originalDef, new ParserContext(this.readerContext, this, containingBd));
				if (decorated != null) {
					return decorated;
				}
			} else if (namespaceUri.startsWith("http://www.springframework.org/")) {
				//若命名空间的uri以"http://www.springframework.org/"开头，则不能为其获取到对应的命名空间处理器
				error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", node);
			} else {
				// A custom namespace, not to be handled by Spring - maybe "xml:...".
				//如果是用户自定义的命名空间，则spring不能为其创建命名空间处理器
				if (logger.isDebugEnabled()) {
					logger.debug("No Spring NamespaceHandler found for XML schema namespace [" + namespaceUri + "]");
				}
			}
		}
		return originalDef;
	}

	@Nullable
	private BeanDefinitionHolder parseNestedCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
		BeanDefinition innerDefinition = parseCustomElement(ele, containingBd);
		if (innerDefinition == null) {
			error("Incorrect usage of element '" + ele.getNodeName() + "' in a nested manner. " +
					"This tag cannot be used nested inside <property>.", ele);
			return null;
		}
		String id = ele.getNodeName() + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR +
				ObjectUtils.getIdentityHexString(innerDefinition);
		if (logger.isTraceEnabled()) {
			logger.trace("Using generated bean name [" + id +
					"] for nested custom element '" + ele.getNodeName() + "'");
		}
		return new BeanDefinitionHolder(innerDefinition, id);
	}


	/**
	 * Get the namespace URI for the supplied node.
	 * <p>The default implementation uses {@link Node#getNamespaceURI}.
	 * Subclasses may override the default implementation to provide a
	 * different namespace identification mechanism.
	 *
	 * @param node the node
	 */
	@Nullable
	public String getNamespaceURI(Node node) {
		return node.getNamespaceURI();
	}

	/**
	 * Get the local name for the supplied {@link Node}.
	 * <p>The default implementation calls {@link Node#getLocalName}.
	 * Subclasses may override the default implementation to provide a
	 * different mechanism for getting the local name.
	 *
	 * @param node the {@code Node}
	 */
	public String getLocalName(Node node) {
		return node.getLocalName();
	}

	/**
	 * Determine whether the name of the supplied node is equal to the supplied name.
	 * <p>The default implementation checks the supplied desired name against both
	 * {@link Node#getNodeName()} and {@link Node#getLocalName()}.
	 * <p>Subclasses may override the default implementation to provide a different
	 * mechanism for comparing node names.
	 *
	 * @param node        the node to compare
	 * @param desiredName the name to check for
	 */
	public boolean nodeNameEquals(Node node, String desiredName) {
		return desiredName.equals(node.getNodeName()) || desiredName.equals(getLocalName(node));
	}

	public boolean isDefaultNamespace(@Nullable String namespaceUri) {
		return (!StringUtils.hasLength(namespaceUri) || BEANS_NAMESPACE_URI.equals(namespaceUri));
	}

	public boolean isDefaultNamespace(Node node) {
		return isDefaultNamespace(getNamespaceURI(node));
	}

	private boolean isCandidateElement(Node node) {
		return (node instanceof Element && (isDefaultNamespace(node) || !isDefaultNamespace(node.getParentNode())));
	}

}
