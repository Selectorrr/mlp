package net.org.selector.mlp.services;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import net.org.selector.mlp.domain.Neuron;
import net.org.selector.mlp.domain.Synapse;
import net.org.selector.mlp.repository.NeuronRepository;
import net.org.selector.mlp.repository.SynapseRepository;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Objects.equal;
import static java.util.Optional.ofNullable;

@Service
public class MlpService {

    @Autowired
    private SynapseRepository synapseRepository;

    @Autowired
    private NeuronRepository neuronRepository;


    private static final String InToHiddenLinks = "inToHidden";
    private static final String HiddenToOutLinks = "hiddenToOut";

    private ImmutableMap<String, Double> linksDefaults = ImmutableMap.of(
            InToHiddenLinks, -0.2,
            HiddenToOutLinks, 0D
    );


    private void generateHiddenNodes(List<Neuron> inputs, List<Neuron> outputs) {
        if (inputs.size() > 3) {
            return;
        }
        List<String> inVals = inputs.stream().map(Neuron::getValue).collect(Collectors.toList());
        String value = Joiner.on('_').join(Ordering.from(String.CASE_INSENSITIVE_ORDER).immutableSortedCopy(inVals));

        Neuron hiddenNeuron = neuronRepository.findOneByTypeAndValue(Neuron.TYPE_HIDDEN, value);
        if (hiddenNeuron == null) {
            hiddenNeuron = new Neuron(Neuron.TYPE_HIDDEN, value);
            neuronRepository.save(hiddenNeuron);

            List<Synapse> synapses = Lists.newArrayList();
            for (Neuron input : inputs) {
                synapses.add(new Synapse(input, hiddenNeuron, 1.0 / inputs.size()));
            }
            for (Neuron output : outputs) {
                synapses.add(new Synapse(hiddenNeuron, output, 0.1));
            }
            synapseRepository.save(synapses);
        }
    }

    private Pair<List<Neuron>, List<Neuron>> resolveNeurons(List<String> inputVals, List<String> outputVals) {
        List<Neuron> inputs = inputVals.stream().map(input -> new Neuron(Neuron.TYPE_IN, input)).collect(Collectors.toList());
        List<Neuron> outputs = outputVals.stream().map(output -> new Neuron(Neuron.TYPE_OUT, output)).collect(Collectors.toList());
        List<String> inVals = inputs.stream().map(Neuron::getValue).collect(Collectors.toList());
        List<String> outVals = outputs.stream().map(Neuron::getValue).collect(Collectors.toList());
        List<Neuron> inputNeurons = neuronRepository.getByTypeAndValueIn(Neuron.TYPE_IN, inVals);
        List<Neuron> outputNeurons = neuronRepository.getByTypeAndValueIn(Neuron.TYPE_OUT, outVals);
        List<Neuron> newInputNeurons = Lists.newArrayList(inputs.stream().filter(a ->
                !Iterables.tryFind(inputNeurons, b ->
                        equal(a.getValue(), b.getValue())).isPresent()).collect(Collectors.toList()));
        List<Neuron> newOutputNeurons = Lists.newArrayList(outputs.stream().filter(a ->
                !Iterables.tryFind(outputNeurons, b ->
                        equal(a.getValue(), b.getValue())).isPresent()).collect(Collectors.toList()));
        neuronRepository.save(Iterables.concat(newInputNeurons, newOutputNeurons));
        Iterables.addAll(inputNeurons, newInputNeurons);
        Iterables.addAll(outputNeurons, newOutputNeurons);
        return new ImmutablePair<>(inputNeurons, outputNeurons);
    }

    private List<Neuron> getHiddenNodes(List<Neuron> inputs, List<Neuron> outputs) {
        List<String> inputsS = Lists.newArrayList(Iterables.transform(inputs, Neuron::getValue));
        List<String> outputsS = Lists.newArrayList(Iterables.transform(outputs, Neuron::getValue));
        List<Neuron> hiddenNodesBetween = neuronRepository.getHiddenNodesBetween(inputsS, outputsS);
        return hiddenNodesBetween == null ? Lists.newArrayList() : hiddenNodesBetween;
    }

    private double[][] getWeights(List<Neuron> from, List<Neuron> to, String linksType) {
        double[][] weights = new double[from.size()][to.size()];
        List<Synapse> synapses = synapseRepository.getBetween(
                from.stream().map(Neuron::getId).collect(Collectors.toList()),
                to.stream().map(Neuron::getId).collect(Collectors.toList())
        );
        int i = 0;
        for (Neuron fromItem : from) {
            int j = 0;
            for (Neuron toItem : to) {
                Optional<Synapse> synapseOptional = Iterables.tryFind(synapses, input -> equal(input.getFrom().getId(), fromItem.getId()) &&
                        equal(input.getTo().getId(), toItem.getId()));

                weights[i][j] = synapseOptional.isPresent() ? synapseOptional.get().getWeight() : linksDefaults.get(linksType);
                j++;
            }
            i++;
        }
        return weights;
    }

    private double[] feedForward(Context context) {
        Arrays.fill(context.ai, 1.0);
        for (int j = 0; j < context.hiddenNodes.size(); j++) {
            double sum = 0.0;
            for (int i = 0; i < context.inputs.size(); i++) {
                sum = sum + context.ai[i] * context.wi[i][j];
            }
            context.ah[j] = Math.tanh(sum);
        }
        for (int k = 0; k < context.outputs.size(); k++) {
            double sum = 0.0;
            for (int j = 0; j < context.hiddenNodes.size(); j++) {
                sum = sum + context.ah[j] * context.wo[j][k];
            }
            context.ao[k] = Math.tanh(sum);
        }
        return context.ao;
    }

    private Context buildContext(List<Neuron> inputs, List<Neuron> outputs) {
        List<Neuron> hiddenNodes = getHiddenNodes(inputs, outputs);
        double[][] wi = getWeights(inputs, hiddenNodes, InToHiddenLinks);
        double[][] wo = getWeights(hiddenNodes, outputs, HiddenToOutLinks);
        return new Context(inputs, outputs, hiddenNodes, wi, wo);
    }

    @Transactional
    public void trainQuery(List<String> inputs, List<String> outputs, String selected) {

        Pair<List<Neuron>, List<Neuron>> resolvedNeurons = resolveNeurons(inputs, outputs);

        List<Neuron> inputNeurons = resolvedNeurons.getLeft();
        List<Neuron> outputNeurons = resolvedNeurons.getRight();

        generateHiddenNodes(inputNeurons, outputNeurons);
        Context context = buildContext(inputNeurons, outputNeurons);
        feedForward(context);
        double[] targets = new double[outputs.size()];
        Arrays.fill(targets, 0.0);
        targets[outputs.indexOf(selected)] = 1.0;
        backPropagate(context, targets);
        updateLinksStrength(context);
    }

    private void updateLinksStrength(Context context) {
        Iterable<Long> inputIds = Iterables.transform(context.inputs, Neuron::getId);
        Iterable<Long> hiddenIds = Iterables.transform(context.hiddenNodes, Neuron::getId);
        Iterable<Long> outputIds = Iterables.transform(context.outputs, Neuron::getId);
        List<Synapse> synapses = synapseRepository.getBetween(Lists.newArrayList(Iterables.concat(inputIds, hiddenIds)), Lists.newArrayList(Iterables.concat(hiddenIds, outputIds)));
        HashBasedTable<Long, Long, Synapse> synapsesByNeurons = HashBasedTable.create();
        for (Synapse synapse : synapses) {
            synapsesByNeurons.put(synapse.getFrom().getId(), synapse.getTo().getId(), synapse);
        }
        List<Synapse> forSave = Lists.newArrayList();
        for (int i = 0; i < context.inputs.size(); i++) {
            for (int j = 0; j < context.hiddenNodes.size(); j++) {
                java.util.Optional<Synapse> synapseOptional = ofNullable(synapsesByNeurons.get(context.inputs.get(i).getId(), context.hiddenNodes.get(j).getId()));
                Synapse synapse = synapseOptional.orElse(new Synapse());
                synapse.setWeight(context.wi[i][j]);
                forSave.add(synapse);
            }
        }
        for (int j = 0; j < context.hiddenNodes.size(); j++) {
            for (int k = 0; k < context.outputs.size(); k++) {
                java.util.Optional<Synapse> synapseOptional = ofNullable(synapsesByNeurons.get(context.hiddenNodes.get(j).getId(), context.outputs.get(k).getId()));
                Synapse synapse = synapseOptional.orElse(new Synapse());
                synapse.setWeight(context.wo[j][k]);
                forSave.add(synapse);
            }
        }
        synapseRepository.save(forSave);
    }

    private void backPropagate(Context context, double[] targets) {
        double[] outputDetails = new double[context.outputs.size()];
        Arrays.fill(outputDetails, 0.0);

        for (int k = 0; k < context.outputs.size(); k++) {
            double error = targets[k] - context.ao[k];
            outputDetails[k] = dtanh(context.ao[k]) * error;
        }
        double[] hiddenDeltas = new double[context.hiddenNodes.size()];
        Arrays.fill(hiddenDeltas, 0.0);

        for (int j = 0; j < context.hiddenNodes.size(); j++) {
            double error = 0.0;
            for (int k = 0; k < context.outputs.size(); k++) {
                error = error + outputDetails[k] * context.wo[j][k];
            }
            hiddenDeltas[j] = dtanh(context.ah[j]) * error;
        }

        for (int j = 0; j < context.hiddenNodes.size(); j++) {
            for (int k = 0; k < context.outputs.size(); k++) {
                double change = outputDetails[k] * context.ah[j];
                context.wo[j][k] = context.wo[j][k] + 0.5 * change;
            }
        }

        for (int i = 0; i < context.inputs.size(); i++) {
            for (int j = 0; j < context.hiddenNodes.size(); j++) {
                double change = hiddenDeltas[j] * context.ai[i];
                context.wi[i][j] = context.wi[i][j] + 0.5 * change;
            }
        }

    }

    private double dtanh(double y) {
        return 1.0 - y * y;
    }

    @Transactional
    public double[] getRanksForOutputs(ImmutableList<String> inputs, ImmutableList<String> outputs) {
        Pair<List<Neuron>, List<Neuron>> resolvedNeurons = resolveNeurons(inputs, outputs);

        List<Neuron> inputNeurons = resolvedNeurons.getLeft();
        List<Neuron> outputNeurons = resolvedNeurons.getRight();

        generateHiddenNodes(inputNeurons, outputNeurons);
        Context context = buildContext(inputNeurons, outputNeurons);
        return feedForward(context);
    }

    private class Context {
        List<Neuron> inputs;
        List<Neuron> outputs;
        List<Neuron> hiddenNodes;
        double[] ai;
        double[] ah;
        double[] ao;
        double[][] wi;
        double[][] wo;

        Context(List<Neuron> inputs, List<Neuron> outputs, List<Neuron> hiddenNodes, double[][] wi, double[][] wo) {
            this.inputs = inputs;
            this.outputs = outputs;
            this.hiddenNodes = hiddenNodes;
            this.ai = new double[inputs.size()];
            this.ah = new double[hiddenNodes.size()];
            this.ao = new double[outputs.size()];
            this.wi = wi;
            this.wo = wo;
            Arrays.fill(this.ai, 0.1);
            Arrays.fill(this.ah, 0.1);
            Arrays.fill(this.ao, 0.1);
        }
    }

}
