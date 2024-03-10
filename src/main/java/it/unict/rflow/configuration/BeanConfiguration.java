package it.unict.rflow.configuration;

import it.unict.rflow.aspect.FlowAspect;
import it.unict.rflow.model.Prediction;
import it.unict.rflow.service.CrudMethodService;
import it.unict.rflow.service.FlowParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Configuration
public class BeanConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public Map<String, Method> mapCrudMethods() {
        final Map<String,Method> methods = new HashMap<>();
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(RestController.class);
        if (beanNames == null) return methods;
        Arrays.stream(beanNames).forEach(beanName -> {
            getRestMethodFromController(beanName, methods);
        });
        return methods;
    }

    private void getRestMethodFromController(String beanName, Map<String,Method> methods) {
        Class<?> controllerClass = applicationContext.getType(beanName);
        Method[] ms = controllerClass.getMethods();
        for (Method m : ms) {
            if (isRestMethod(m)) {
                methods.put(m.getName(), m);
            }
        }
    }

    private boolean isRestMethod(Method m) {
        return m.isAnnotationPresent(GetMapping.class) || m.isAnnotationPresent(PostMapping.class)
                || m.isAnnotationPresent(DeleteMapping.class) || m.isAnnotationPresent(PutMapping.class);
    }

    /*@Bean
    public FlowAspect flowAspect() {
        return new FlowAspect();
    }

    @Bean
    public CrudMethodService crudMethodService() {
        return new CrudMethodService();
    }*/

    @Value("${flow:}")
    private String flowInAppProperties;

    @Bean
    public Set<Prediction> predictions(FlowParser flowParser) {
        return flowParser.flowsParsing(flowInAppProperties);
    }
    /*
    @Bean
    public FlowParser flowParser() {
        return new FlowParser();
    }*/

}
