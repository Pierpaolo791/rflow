package it.unict.rflow.aspect;

import it.unict.rflow.model.Prediction;
import it.unict.rflow.service.CrudMethodService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.stream.Stream;

import static it.unict.rflow.aspect.FlowAspect.stop;

@Aspect
@Component
public class MonitoringAspect {

    @Value("${rflow.sessioncookiename:rflowsession}")
    private String sessionCookieName;

    @Value("#{new Boolean('${rflow.prediction:false}')}")
    private Boolean predict;

    @Autowired
    private CrudMethodService crudMethodService;

    public static Map<String,Prediction> predictionMap = new HashMap<>();
    public static Map<String,List<String>> countActionUser = new HashMap<>();

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void pointcutController() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PostMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PatchMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void pointcutMethodController() {}

    @After("pointcutController() && pointcutMethodController()")
    public void countRequest(JoinPoint joinPoint) throws Exception {
        if (stop.get() || !predict) return;
        Cookie cookie = getSessionCookie();
        List<String> list = new ArrayList<>();
        list.add(joinPoint.getStaticPart().getSignature().getName());
        if(!countActionUser.containsKey(cookie.getValue())) countActionUser.put(cookie.getValue(),list);
        else countActionUser.get(cookie.getValue()).addAll(list);
    }

    @Before("pointcutController() && pointcutMethodController()")
    public void getPrediction(JoinPoint joinPoint) throws Exception {
        if (stop.get() || !predict) return;
        String sourceMethodName = joinPoint.getStaticPart().getSignature().getName();
        Cookie cookie = getSessionCookie();
        List<String> requests = countActionUser.get(cookie.getValue());
        Prediction prediction = algorithm(requests, sourceMethodName);
        predictionMap.put(cookie.getValue(), prediction);
    }

    private Prediction algorithm(List<String> requests, String sourceMethodName) {
        List<String> nextRequests = new LinkedList<>();
        if (requests == null || requests.isEmpty() || requests.size() < 2) return null;
        for (int i=0; i<requests.size()-1; i++)
            if (requests.get(i).equals(sourceMethodName))
                nextRequests.add(requests.get(i+1));
        if (nextRequests.isEmpty() || nextRequests.size() < 2) return null;
        String maxPrediction = getMethodNameMaxPrediction(nextRequests);
        if (maxPrediction.isEmpty()) return null;
        return  Prediction.builder().source(sourceMethodName)
                .target(crudMethodService.getMethodByName(maxPrediction)).build();
    }

    private String getMethodNameMaxPrediction(List<String> nextRequests) {
        Map<String, Integer> occurrences = new HashMap<>();
        for (String element : nextRequests) {
            occurrences.put(element, occurrences.getOrDefault(element, 0) + 1);
        }
        String maxPrediction = "";
        Integer max = 0;
        for (Map.Entry<String, Integer> entry : occurrences.entrySet()) {
            if (entry.getValue() > max) {
                maxPrediction = entry.getKey();
                max = entry.getValue();
            }
        }
        return maxPrediction;
    }


    public Cookie getSessionCookie() throws Exception {
        return Optional.ofNullable(getCurrentRequest().getCookies())
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .filter(x -> x.getName().equals(sessionCookieName))
                .findAny().orElseThrow(Exception::new);
    }

    public Cookie getCookieNewSessionWithRandomUuid() {
        Cookie cookie = new Cookie(sessionCookieName,generateRandomUuid());
        return cookie;
    }

    private HttpServletRequest getCurrentRequest() {
        return getServletRequestAttributes().getRequest();
    }

    private String generateRandomUuid() {
        return UUID.randomUUID().toString();
    }

    private Cookie[] getCookiesFromCurrentRequest() {
        if(getServletRequestAttributes() == null) return null;
        HttpServletRequest request = getServletRequestAttributes().getRequest();
        return request.getCookies();
    }


    private HttpServletResponse getHttpServletResponseFromCurrentRequest() {
        ServletRequestAttributes servletRequestAttributes = getServletRequestAttributes();
        if (servletRequestAttributes == null) return null;
        return getServletRequestAttributes().getResponse();
    }

    private ServletRequestAttributes getServletRequestAttributes() {
        if (RequestContextHolder.currentRequestAttributes() == null) return null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes;
    }

}
