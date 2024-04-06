package it.unict.rflow.model;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Method;
@Setter
@Getter
@NoArgsConstructor
public class Action {

    private String sessionId;
    private Method method;
    private HttpServletRequest request;
    private String requestId;
    private Object objectResponse;
    private String targetMethodName;

}
