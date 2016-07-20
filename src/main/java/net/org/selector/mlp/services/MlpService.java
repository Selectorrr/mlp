package net.org.selector.mlp.services;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import net.org.selector.mlp.domain.Context;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Selector on 20.07.2016.
 */
@Service
public class MlpService {

    private static final String InToHiddenLinks = "inToHidden";
    private static final String HiddenToOutLinks = "hiddenToOut";

    private ImmutableMap<String, Table<String, String, Double>> links = ImmutableMap.of(
            InToHiddenLinks, HashBasedTable.create(),
            HiddenToOutLinks, HashBasedTable.create()
    );
    private ImmutableMap<String, Double> linksDefaults = ImmutableMap.of(
            InToHiddenLinks, -0.2,
            HiddenToOutLinks, 0D
    );

    private Set<String> hiddenNodes = Sets.newHashSet();

    private double getLinkStrength(String from, String to, String linksType) {
        Double link = links.get(linksType).get(from, to);
        return link != null ? link : linksDefaults.get(linksType);
    }

    private void setLinkStrength(String from, String to, String linksType, Double val) {
        links.get(linksType).put(from, to, val);
    }

    private void generateHiddenNodes(List<String> inputs, List<String> outputs) {
        if (inputs.size() > 3) {
            return;
        }
        ImmutableList<String> orderedInputs = Ordering.from(String.CASE_INSENSITIVE_ORDER).immutableSortedCopy(inputs);
        String key = Joiner.on('_').join(orderedInputs);
        if (hiddenNodes.add(key)) {
            for (String input : inputs) {
                setLinkStrength(input, key, InToHiddenLinks, 1.0 / inputs.size());
            }
            for (String output : outputs) {
                setLinkStrength(key, output, HiddenToOutLinks, 0.1);
            }
        }
    }

    private List<String> getHiddenNodes(List<String> inputs, List<String> outputs) {
        Set<String> result = Sets.newHashSet();
        Table<String, String, Double> inToHiddenLinks = links.get(InToHiddenLinks);
        for (String input : inputs) {
            Map<String, Double> hiddenNodeToVal = inToHiddenLinks.row(input);
            result.addAll(hiddenNodeToVal.keySet());
        }
        Table<String, String, Double> hiddenToOutLinks = links.get(HiddenToOutLinks);
        for (String output : outputs) {
            Map<String, Double> hiddenNodeToVal = hiddenToOutLinks.column(output);
            result.addAll(hiddenNodeToVal.keySet());
        }
        return Lists.newArrayList(result);
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

    public void trainQuery(List<String> inputs, List<String> outputs, String selected) {
        generateHiddenNodes(inputs, outputs);
        Context context = buildContext(inputs, outputs);
        feedForward(context);
        double[] targets = new double[outputs.size()];
        Arrays.fill(targets, 0.0);
        targets[outputs.indexOf(selected)] = 1.0;
        backPropagate(context, targets);
        updateLinksStrength(context);
    }

    private void updateLinksStrength(Context context) {
        for (int i = 0; i < context.inputs.size(); i++) {
            for (int j = 0; j < context.hiddenNodes.size(); j++) {
                setLinkStrength(context.inputs.get(i), context.hiddenNodes.get(j), InToHiddenLinks, context.wi[i][j]);
            }
        }
        for (int j = 0; j < context.hiddenNodes.size(); j++) {
            for (int k = 0; k < context.outputs.size(); k++) {
                setLinkStrength(context.hiddenNodes.get(j), context.outputs.get(k), HiddenToOutLinks, context.wo[j][k]);
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

    public double[] getRanksForOutputs(ImmutableList<String> inputs, ImmutableList<String> outputs) {
        generateHiddenNodes(inputs, outputs);
        Context context = buildContext(inputs, outputs);
        return feedForward(context);
    }

}
