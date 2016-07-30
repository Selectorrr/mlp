package net.org.selector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.org.selector.model.mlp.Neuron;
import net.org.selector.model.mlp.Synapse;
import net.org.selector.repository.mpl.NeuronRepository;
import net.org.selector.repository.mpl.SynapseRepository;
import net.org.selector.services.mlp.MlpService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MlpApplication.class)
@WebAppConfiguration
public class MlpApplicationTests {

    @Autowired
    private MlpService mlpService;

    @Autowired
    private NeuronRepository neuronRepository;

    @Autowired
    private SynapseRepository synapseRepository;

    private ImmutableList<String> inputs;
    private ImmutableList<String> outputs;

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

    @Test
    public void findByTypeAndValueIn() {
        Neuron neuron = new Neuron(Neuron.TYPE_IN, "val1");
        neuronRepository.save(neuron);
        List<Neuron> result = neuronRepository.getByTypeAndValueIn(Neuron.TYPE_IN, Lists.newArrayList("val1"));
        Assert.assertEquals("find by type and value", 1, result.size());
    }

    @Test
    public void getBetween() {
        Synapse s = synapseRepository.save(new Synapse(new Neuron(), new Neuron(), 1));
        List<Synapse> r = synapseRepository.getBetween(Collections.singletonList(s.getFrom().getId()), Collections.singletonList(s.getTo().getId()));
        Assert.assertEquals("getBetween", 1, r.size());
    }

}
