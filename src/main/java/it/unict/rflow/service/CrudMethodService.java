package it.unict.rflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Map;

@Service
public class CrudMethodService {

    @Autowired
    private Map<String, Method> mapCrudMethods;


    public Method getMethodByName(String methodName) {
        return mapCrudMethods.get(methodName);
    }

}
