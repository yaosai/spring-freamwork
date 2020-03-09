package org.springframework.context.annotation;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author YaoS
 * @date 2020/3/4 17:16
 */
public class MySpringTests {

	@Test
	public static void main(String[] args) {
		//从java注解的配置中加载配置到容器
		ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
		//从容器中获取对象实例
		Man man = (Man) context.getBean("man1");
		man.driveCar();
	}
}

@Component
class Man {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void driveCar() {
		System.out.println("man " + name + " driveCar");
	}
}

@Configuration
class AppConfig {
	@Bean(name = "man1")
	public Man man1() {
		Man man = new Man();
		man.setName("Steve");
		return man;
	}

	@Bean(name = "man2")
	public Man man2() {
		Man man = new Man();
		man.setName("Jack");
		return man;
	}
}

