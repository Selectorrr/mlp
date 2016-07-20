package net.org.selector.mlp;

import com.google.common.collect.ImmutableList;
import net.org.selector.mlp.domain.Context;
import net.org.selector.mlp.services.MlpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@SpringBootApplication
public class MlpApplication {
    @Autowired
    private MlpService mlpService;

    public static void main(String[] args) {
        SpringApplication.run(MlpApplication.class, args);
    }

    @PostConstruct
    public void postConstruct() {
        ImmutableList<String> inputs = ImmutableList.of("wWorld", "wBank");
        ImmutableList<String> outputs = ImmutableList.of("uWorldBank", "uRiver", "uEarth");

        double[] result = mlpService.getRanksForOutputs(inputs, outputs);
        System.out.println(Arrays.toString(result));

        String selected = "uWorldBank";
        mlpService.trainQuery(inputs, outputs, selected);
        double[] result2 = mlpService.getRanksForOutputs(inputs, outputs);
        System.out.println(Arrays.toString(result2));
    }




}
