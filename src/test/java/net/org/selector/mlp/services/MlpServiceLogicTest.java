package net.org.selector.mlp.services;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.org.selector.mlp.MlpApplication;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by Stepan Litvinov on 2019-08-18.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MlpApplication.class)
@Slf4j
public class MlpServiceLogicTest {

    private ImmutableList<String> inputs;
    private ImmutableList<String> outputs;
    @Autowired
    private MlpService mlpService;

    @Before
    public void init() {
        inputs = ImmutableList.of("wWorld", "wBank");
        outputs = ImmutableList.of("uWorldBank", "uRiver", "uEarth");
    }

    @Test
    public void mlpTest() {
        double[] result = mlpService.getRanksForOutputs(inputs, outputs);
        Assert.assertArrayEquals("rank", new double[]{0.07601250837541616, 0.07601250837541616, 0.07601250837541616}, result, 0.000001);

        String selected = "uWorldBank";
        mlpService.trainQuery(inputs, outputs, selected);
        double[] result2 = mlpService.getRanksForOutputs(inputs, outputs);
        Assert.assertArrayEquals("after train", new double[]{0.33506294978429246, 0.05512695704100274, 0.05512695704100274}, result2, 0.000001);
    }


}