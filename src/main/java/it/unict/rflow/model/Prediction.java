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

    private final String source;
    private final Method target;

    @Override
    public boolean equals(Object o) {
        if (o instanceof Prediction ) {
            Prediction p = (Prediction) o;
            if (p == null || p.getSource() == null || p.getTarget() == null) return false;
            if (p.source.equals(this.source) && p.target.equals(this.target) ) return true;
        }
        return false;
    }

}
