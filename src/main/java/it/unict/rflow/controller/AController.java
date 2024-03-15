package it.unict.rflow.controller;

import it.unict.rflow.aspect.FlowAspect;
import it.unict.rflow.model.Action;
import it.unict.rflow.model.Prediction;
import it.unict.rflow.service.CrudMethodService;
import it.unict.rflow.service.FlowParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class AController {

    @GetMapping("/a")
    public String getA() throws InterruptedException {
        System.out.println("Invoke A..");
        Thread.sleep(5000);
        System.out.println("Return A..");
        return "A";
    }

    @GetMapping("/b")
    public String getB() throws InterruptedException {
        return "B";
    }

    @GetMapping("/c")
    public ResponseEntity<String> getC() {
        System.out.println("Invoke C...");
        //Operation
        System.out.println("Return C...");
        return ResponseEntity.ok("C");
    }


    @GetMapping("/d/{nome}")
    public ResponseEntity<String> getD(@PathVariable String nome) {
        System.out.println("Invoke D...");
        //Operation
        System.out.println("Return D...");
        return ResponseEntity.ok(nome);
    }

    @GetMapping("/e")
    public ResponseEntity<String> getE(String cognome, BigInteger eta, String nome) {
        System.out.println("Invoke E...");
        //Operation
        System.out.println("Return E...");
        return ResponseEntity.ok(nome);
    }


}
