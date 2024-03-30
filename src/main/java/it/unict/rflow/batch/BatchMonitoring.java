package it.unict.rflow.batch;

import it.unict.rflow.aspect.FlowAspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigInteger;

@Configuration
@EnableScheduling
public class BatchMonitoring {

    @Scheduled(fixedDelayString = "${rflow.refreshtime:600000}")
    public void schedule() {
        FlowAspect.countActionUser.clear();
        System.out.println("LOGGING: pulita la mappa delle azioni");
    }

}
