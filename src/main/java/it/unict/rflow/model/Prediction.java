package it.unict.rflow.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.List;

@Builder
@Getter
@Setter
public class Prediction {


    private final List<String> sources;
    private final Method target;

}
