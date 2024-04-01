package it.unict.rflow.aspect;

import it.unict.rflow.model.Action;
import it.unict.rflow.model.Prediction;
import it.unict.rflow.service.CrudMethodService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Aspect
@Component
public class FlowAspect {

    @Value("${rflow.sessioncookiename:aopSessionId}")
    private String sessionCookieName;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CrudMethodService crudMethodService;

    @Autowired
    private Set<Prediction> predictions;

    public static Map<String,Prediction> predictionMap = new HashMap<>();

    public static Map<String,List<String>> countActionUser = new HashMap<>();

    private List<Action> actions = new LinkedList<>();

    private Map<String,Prediction> calculatedPrediction = new HashMap<>();


    @Value("#{new Boolean('${rflow.predict:false}')}")
    private Boolean predict;

    public static AtomicBoolean stop = new AtomicBoolean(false);


    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void pointcutController() {}


    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PostMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PatchMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void pointcutMethodController() {}

    @Before("pointcutController() && pointcutMethodController()")
    public void getPrediction(JoinPoint joinPoint) throws Exception {

        if (stop.get() && !predict.booleanValue()) return;
        //if (countActionUser. < 6) return;
        Cookie cookie = getCookieSession();
        List<String> requests = countActionUser.get(cookie.getValue());
        List<String> nextRequests = new LinkedList<>();
        if (requests == null || requests.isEmpty() || requests.size() < 2) return;
        for (int i=0; i<requests.size()-1; i++)
            if (requests.get(i).equals(joinPoint.getStaticPart().getSignature().getName()))
                nextRequests.add(requests.get(i+1));
        Map<String, Integer> occurrences = new HashMap<>();
        if (nextRequests == null || nextRequests.isEmpty() || nextRequests.size() < 2) return;
        for (String element : nextRequests) {
            occurrences.put(element, occurrences.getOrDefault(element, 0) + 1);
        }
        // Stampiamo le occorrenze
        String maxPrediction = new String();
        Integer max = 0;
        for (Map.Entry<String, Integer> entry : occurrences.entrySet()) {
            if (entry.getValue() > max) {
                maxPrediction = entry.getKey();
                max = entry.getValue();
            }
            System.out.println("Elemento: " + entry.getKey() + ", Occorrenze: " + entry.getValue());
        }
        Prediction prediction = Prediction.builder().source( joinPoint.getStaticPart().getSignature().getName())
                        .target(crudMethodService.getMethodByName(maxPrediction)).build();
        predictionMap.put(cookie.getValue(), prediction);
        //if (!predictions.contains(prediction)) predictions.add(prediction);
    }

    @Around("pointcutController() && pointcutMethodController()")
    public Object readCookie(ProceedingJoinPoint joinPoint) throws Throwable {
        if (stop.get()) return joinPoint.proceed();
        try {
            final Cookie sessionCookie = getSessionCookie();
            Optional<Action> actionTarget =
                    actions.stream().filter(action -> action.getTargetMethodName().equals(joinPoint.getSignature().getName()) &&
                            action.getSessionId().equals(sessionCookie.getValue())).findAny();
            if (actionTarget.isPresent()) {
                Action action = actionTarget.get();
                final Object response = action.getObjectResponse();
                //Spiegare al prof il removeIf in questo caso e del perché potrebbe essere utile
                actions.removeIf(act -> act.getTargetMethodName().equals(joinPoint.getSignature().getName()) &&
                        act.getSessionId().equals(sessionCookie.getValue()));
                actions.remove(action);
                return response;
            }

        } catch(Exception e) {
            if (getHttpServletResponseFromCurrentRequest() != null)
                getHttpServletResponseFromCurrentRequest().addCookie(getCookieByNewSessionWithRandomUuid());
            System.out.println("Settato un nuovo cookie di sessione all'utente corrente");
        }
        return joinPoint.proceed();
    }
    @After("pointcutController() && pointcutMethodController()")
    public void countRequest(JoinPoint joinPoint) throws Exception {
        if (stop.get() && !predict.booleanValue()) return;
        Cookie cookie = getCookieSession();
        List<String> list = new ArrayList<>();
        list.add(joinPoint.getStaticPart().getSignature().getName());
        if(!countActionUser.containsKey(cookie.getValue())) countActionUser.put(cookie.getValue(),list);
        else countActionUser.get(cookie.getValue()).addAll(list);
    }

    @After("pointcutController() && pointcutMethodController()")
    public void prepareA(JoinPoint joinPoint) throws Exception {
        if (stop.get()) return;
        checkPredictionAndRun(joinPoint.getSignature().getName(), joinPoint.getArgs());
    }

    private void checkPredictionAndRun(String methodName, Object[] args) {
        predictions.stream().filter(x -> x.getSource().equals(methodName)).findAny().ifPresent(
                prediction -> {
                    Method m = prediction.getTarget();
                    generateAction(methodName, args, m);
                }
        );
        try {
            Cookie cookie = getSessionCookie();
            Prediction p = predictionMap.get(cookie.getValue());
            if (p != null && p.getSource().equals(methodName))
                generateAction(methodName,args, p.getTarget());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void generateAction(String methodName, Object[] args, Method m) {
        Action action = initAction(methodName, args, m);
        Object[] params = getParamsShared(m, methodName, args);
        CompletableFuture.runAsync( () -> {
            try {
                stop.set(true);
                action.setObjectResponse(m.invoke(applicationContext.getBean(m.getDeclaringClass()),params));
                stop.set(false);
                actions.add(action);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Action initAction(String methodName, Object[] args, Method m) {
        Action action = new Action();
        action.setRequest(getCurrentRequest());
        try {
            action.setSessionId(getCookieSession().getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        action.setRequest(getCurrentRequest());
        action.setMethod(m);
        action.setTargetMethodName(m.getName());
        return action;
    }

    private Object[] getParamsShared(Method target, String methodNameSource, Object[] sourceValue) {
        Parameter[] paramsSource = crudMethodService.getMethodByName(methodNameSource).getParameters();
        Parameter[] paramsTarget = target.getParameters();
        Object[] targetValue = getValueOfTarget(sourceValue, paramsTarget, paramsSource);
        return targetValue;
    }

    private Object[] getValueOfTarget(Object[] sourceValue, Parameter[] paramsTarget, Parameter[] paramsSource) {
        Object[] targetValue = new Object[paramsTarget.length];
        for (int i = 0; i< paramsTarget.length; i++)
            for (int j = 0; j< paramsSource.length; j++)
                if(paramsTarget[i].getName().equals(paramsSource[j].getName()) && paramsTarget[i].getType().equals(paramsSource[j].getType()))
                    targetValue[i] = sourceValue[j];
        return targetValue;
    }

    private Cookie getCookieSession() throws Exception {
        return Optional.ofNullable(getCurrentRequest().getCookies())
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .filter(x -> x.getName().equals(sessionCookieName))
                .findAny().orElseThrow(Exception::new);
    }

    private Cookie getSessionCookie() throws Exception {
        return Arrays.stream(getCookiesFromCurrentRequest())
                .filter(cookie -> cookie.getName().equals(sessionCookieName))
                .findAny().orElseThrow(Exception::new);
    }

    private Cookie getCookieByNewSessionWithRandomUuid() {
        Cookie cookie = new Cookie(sessionCookieName,generateRandomUuid());
        return cookie;
    }


    private Cookie ifNotPresentSetCookieInHttpResponseAndReturn() {
        Cookie cookie = getCookieByNewSessionWithRandomUuid();
        getHttpServletResponseFromCurrentRequest().addCookie(cookie);
        return cookie;
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

    private HttpServletRequest getCurrentRequest() {
        return getServletRequestAttributes().getRequest();
    }

    private String generateRandomUuid() {
        return UUID.randomUUID().toString();
    }

}
