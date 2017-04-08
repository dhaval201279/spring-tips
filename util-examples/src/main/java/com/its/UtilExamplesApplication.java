package com.its;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.*;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.WebUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class UtilExamplesApplication {

	@Aspect
	@Component
	public static class SimpleBeforeAspect {
		@Before("execution(* begin(..))")
		public void before(JoinPoint joinPoint) {
			log.info("==============================");
			log.info("before()");
			log.info("Signature : " + joinPoint.getSignature());
		}
	}

	@Data
	@AllArgsConstructor
	//@NoArgsConstructor
	public static class DemoClass {
		@PostConstruct
		public void begin() {
			log.info("Begin : ");
		}

		private final List<Map<String, Object>> list = new ArrayList<>();
	}

	@Bean
	DemoClass demoClass() {
		return new DemoClass();
	}
	@Bean
	CommandLineRunner demo(DemoClass demo) {
		//demo.setList(null);

		return args -> {
			Assert.notNull(demo.getList(), "The list can't be null");

			beanUtils(demo);
			
			classUtils();

			systemPropertyUtils();

			fileCopyUtils();

			web();

			aop(demo);

			reflection();
		};
	}

	private void reflection() {
		ReflectionUtils.doWithFields(DemoClass.class, field -> log.info("field : " + field.toString()));
		ReflectionUtils.doWithMethods(DemoClass.class, method -> log.info("method = " + method.toString()));
		Field list  = ReflectionUtils.findField(DemoClass.class,"list");
		log.info(list.toString());

		ResolvableType rt = ResolvableType.forField(list);
		log.info("Resolvable type : " + 	rt.toString());
	}

	private void aop(DemoClass demo) {
		Class<?> targetClass = AopUtils.getTargetClass(demo);
		log.info("Class is : " + targetClass);
		log.info("Is AOP Proxy : " + AopUtils.isAopProxy(demo));
		log.info("Is CG Lib Proxy : " + AopUtils.isCglibProxy(demo));
	}

	private void web() {
		RestTemplate rt = new RestTemplate();
		rt.getForEntity("http://localhost:8080/hi", Void.class);
	}

	@RestController
	public static class SimpleRestController {
		@GetMapping("/hi")
		void hi(HttpServletRequest request) {
			long age = ServletRequestUtils.getIntParameter(request,"age", -1);
			log.info("Age is : " + age);
			File tempDir = WebUtils.getTempDir(request.getServletContext());
			log.info("Temporary directory Apach Tomcat : " + tempDir.getAbsolutePath());
			WebApplicationContext webApplicationContext = RequestContextUtils.findWebApplicationContext(request);
			Environment bean = webApplicationContext.getBean(Environment.class);
			log.info("Web application context resolved property : " + bean.getProperty("user.home"));
		}
	}

	private void fileCopyUtils() {
		File file = new File(SystemPropertyUtils.resolvePlaceholders("${user.home}"),"\\temp\\content.txt");
		try (Reader reader = new FileReader(file)){
			log.info("Contents of file : " + FileCopyUtils.copyToString(reader));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}


	private void classUtils() {
		Constructor<DemoClass> demoClassConstructor = ClassUtils.getConstructorIfAvailable(DemoClass.class);
		log.info("Demo Class Constructor : " + demoClassConstructor);

		try {
			DemoClass demoClass = demoClassConstructor.newInstance();
			log.info("New Instance demo class : " + demoClass);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void systemPropertyUtils() {
		String resolvedText = SystemPropertyUtils.resolvePlaceholders("My home directory is ${user.home}");
		log.info("Resolved Text : " + resolvedText);

	}
	private void beanUtils(DemoClass demo) {
		PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(demo.getClass());
		for (PropertyDescriptor pd : descriptors) {
			log.info("pd : " + pd.getName());
			//log.info("pd.readMethod : " + pd.getReadMethod().getName());
		}
		
	}

	private static final Log log = LogFactory.getLog(UtilExamplesApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(UtilExamplesApplication.class, args);
	}
}
