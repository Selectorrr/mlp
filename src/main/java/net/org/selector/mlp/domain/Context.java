package net.org.selector.mlp.domain;

/**
 * Created by Selector on 20.07.2016.
 */

import java.util.Arrays;
import java.util.List;

public class Context {
    public List<String> inputs;
    public List<String> outputs;
    public List<String> hiddenNodes;
    public double[] ai;
    public double[] ah;
    public double[] ao;
    public double[][] wi;
    public double[][] wo;

    public Context(List<String> inputs,
                   List<String> outputs,
                   List<String> hiddenNodes, double[][] wi, double[][] wo) {
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
