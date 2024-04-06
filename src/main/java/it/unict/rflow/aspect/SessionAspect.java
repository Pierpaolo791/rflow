package it.unict.rflow.aspect;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;

import static it.unict.rflow.aspect.FlowAspect.stop;

@Aspect
@Component
public class SessionAspect {

    @Value("${rflow.sessioncookiename:aopSessionId}")
    private String sessionCookieName;

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void pointcutController() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PostMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PatchMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping)" +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void pointcutMethodController() {}

    @Around("pointcutController() && pointcutMethodController()")
    public Object setCookieIfNotPresent(ProceedingJoinPoint joinPoint) throws Throwable {
        if (stop.get()) return joinPoint.proceed();
        try {
            getSessionCookie();
        } catch(Exception e) {
            HttpServletResponse servletResponse = getHttpServletResponseFromCurrentRequest();
            if (servletResponse != null)
                servletResponse.addCookie(getCookieNewSessionWithRandomUuid());
        }
        return joinPoint.proceed();
    }

    public Cookie getSessionCookie() throws Exception {
        return Arrays.stream(getCookiesFromCurrentRequest())
                .filter(cookie -> cookie.getName().equals(sessionCookieName))
                .findAny().orElseThrow(Exception::new);
    }

    public Cookie getCookieNewSessionWithRandomUuid() {
        Cookie cookie = new Cookie(sessionCookieName,generateRandomUuid());
        return cookie;
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
