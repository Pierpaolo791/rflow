package it.unict.rflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class RflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(RflowApplication.class, args);
	}

}
