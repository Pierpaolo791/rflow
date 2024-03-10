package it.unict.rflow.service;


import it.unict.rflow.model.Prediction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FlowParser {

    @Autowired
    private Map<String, Method> mapCrudMethods;

    public FlowParser() {
    }

    public Set<Prediction> flowsParsing(String flows) {
        if (flows == null || flows.isEmpty()) return new HashSet<>();
        List<String> listOfFlow = Arrays.asList(flows.split(";"));
        return listOfFlow.stream().map( flow -> getPredictionByFlowString(flow)).collect(Collectors.toSet());
    }

    private Prediction getPredictionByFlowString(String flow) {
        String[] flowSplitted = flow.split("->");
        List<String> sources = Arrays.asList(flowSplitted[0].split(",")).stream().collect(Collectors.toList());
        Method target = mapCrudMethods.get(flowSplitted[1]);
        return Prediction.builder().sources(sources).target(target).build();
    }


}
