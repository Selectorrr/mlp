package net.org.selector.mlp.services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import net.org.selector.mlp.domain.Context;
import net.org.selector.mlp.domain.NodeType;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Selector on 20.07.2016.
 */
@Service
public class MlpService {

    private static final String InToHiddenLinks = "inToHidden";
    private static final String HiddenToOutLinks = "hiddenToOut";

    private ImmutableMap<String, Double> linksDefaults = ImmutableMap.of(
            InToHiddenLinks, -0.2,
            HiddenToOutLinks, 0D
    );

    private final MutableValueGraph<String, Double> brain = ValueGraphBuilder
            .undirected()
            .allowsSelfLoops(false)
            .build();


    public void trainQuery(List<String> inputArgs, List<String> outputArgs, String selectedArg) {
        if (inputArgs.isEmpty()) {
            return;
        }

        List<String> inputs = toNode(inputArgs, NodeType.INPUT);

        List<String> outputs = toNode(outputArgs, NodeType.OUTPUT);

        String selected = selectedArg + NodeType.OUTPUT;

        synchronized (brain) {
            generateHiddenNodes(inputs, outputs);
            Context context = buildContext(inputs, outputs);
            feedForward(context);
            double[] targets = new double[outputs.size()];
            Arrays.fill(targets, 0.0);
            targets[outputs.indexOf(selected)] = 1.0;
            backPropagate(context, targets);
            updateLinksStrength(context);
        }
    }

    public double[] getRanksForOutputs(ImmutableList<String> inputArgs, ImmutableList<String> outputArgs) {
        List<String> inputs = toNode(inputArgs, NodeType.INPUT);

        List<String> outputs = toNode(outputArgs, NodeType.OUTPUT);

        synchronized (brain) {
            generateHiddenNodes(inputs, outputs);
            Context context = buildContext(inputs, outputs);
            return feedForward(context);
        }
    }

    private List<String> toNode(List<String> inputArgs, NodeType type) {
        return inputArgs
                .stream()
                .parallel()
                .map(i -> i + type)
                .distinct()
                .collect(Collectors.toList());
    }

    private double getLinkStrength(String from, String to, String linksType) {
        return Objects.requireNonNull(brain.edgeValueOrDefault(from, to, linksDefaults.get(linksType)));
    }

    private void generateHiddenNodes(List<String> inputs, List<String> outputs) {
//        if (inputs.size() > 3) {
//            return;
//        }

        String keyName = String.join("_", Ordering.from(String.CASE_INSENSITIVE_ORDER).immutableSortedCopy(inputs));

        String key = keyName + NodeType.HIDDEN;
        addHiddenNodes(inputs, outputs, key);
    }

    private void addHiddenNodes(List<String> inputs, List<String> outputs, String key) {
        if (brain.addNode(key)) {
            for (String input : inputs) {
                brain.putEdgeValue(input, key, 1.0 / inputs.size());
            }
            for (String output : outputs) {
                brain.putEdgeValue(key, output, 0.1);
            }
        }
    }

    private List<String> getHiddenNodes(List<String> inputs, List<String> outputs) {

        Set<String> inNodes = inputs
                .stream()
                .parallel()
                .flatMap(i -> brain.adjacentNodes(i).stream())
                .collect(Collectors.toSet());

        return outputs
                .stream()
                .parallel()
                .flatMap(i -> brain.adjacentNodes(i).stream())
                .filter(inNodes::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    private double[][] getWeights(List<String> from, List<String> to, String linksType) {
        double[][] weights = new double[from.size()][to.size()];
        int i = 0;
        for (String fromItem : from) {
            int j = 0;
            for (String toItem : to) {
                weights[i][j] = getLinkStrength(fromItem, toItem, linksType);
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

    private Context buildContext(List<String> inputs, List<String> outputs) {
        List<String> hiddenNodes = getHiddenNodes(inputs, outputs);
        double[][] wi = getWeights(inputs, hiddenNodes, InToHiddenLinks);
        double[][] wo = getWeights(hiddenNodes, outputs, HiddenToOutLinks);
        return new Context(inputs, outputs, hiddenNodes, wi, wo);
    }

    private void updateLinksStrength(Context context) {
        for (int i = 0; i < context.inputs.size(); i++) {
            for (int j = 0; j < context.hiddenNodes.size(); j++) {
                brain.putEdgeValue(context.inputs.get(i), context.hiddenNodes.get(j), context.wi[i][j]);
            }
        }
        for (int j = 0; j < context.hiddenNodes.size(); j++) {
            for (int k = 0; k < context.outputs.size(); k++) {
                brain.putEdgeValue(context.hiddenNodes.get(j), context.outputs.get(k), context.wo[j][k]);
            }
        }
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

}
