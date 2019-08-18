package net.org.selector.mlp;

import net.org.selector.mlp.services.MlpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MlpApplication {
    @Autowired
    private MlpService mlpService;

    public static void main(String[] args) {
        SpringApplication.run(MlpApplication.class, args);
    }

//    @PostConstruct
//    public void postConstruct() {
//        ImmutableList<String> inputs = ImmutableList.of("1", "2"); //поставленные лайки
//        ImmutableList<String> outputs = ImmutableList.of("3", "4", "5"); //нужно лайкнуть
//
//        double[] result = mlpService.getRanksForOutputs(inputs, outputs);
//        System.out.println(Arrays.toString(result));
//
//        String selected = "4"; //лайкнул
//        mlpService.trainQuery(inputs, outputs, selected);
//        double[] result2 = mlpService.getRanksForOutputs(inputs, ImmutableList.of("3", "4"));
//        System.out.println(Arrays.toString(result2));
//
//        double[] result3 = mlpService.getRanksForOutputs(ImmutableList.of("2"), ImmutableList.of("1", "4"));
//        System.out.println(Arrays.toString(result3));
//
//    }




}
