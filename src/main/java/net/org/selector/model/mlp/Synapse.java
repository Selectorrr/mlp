package net.org.selector.model.mlp;

import org.neo4j.ogm.annotation.*;

/**
 * Created by Selector on 29.07.2016.
 */
@RelationshipEntity(type = "LINKED")
public class Synapse {
    @GraphId
    private Long id;
    @StartNode
    private Neuron from;
    @EndNode
    private Neuron to;
    @Property
    private double weight;

    public Synapse() {
    }

    public Synapse(Neuron from, Neuron to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Neuron getFrom() {
        return from;
    }

    public void setFrom(Neuron from) {
        this.from = from;
    }

    public Neuron getTo() {
        return to;
    }

    public void setTo(Neuron to) {
        this.to = to;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Synapse synapse = (Synapse) o;

        if (Double.compare(synapse.weight, weight) != 0) return false;
        if (id != null ? !id.equals(synapse.id) : synapse.id != null) return false;
        if (from != null ? !from.equals(synapse.from) : synapse.from != null) return false;
        return to != null ? to.equals(synapse.to) : synapse.to == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id != null ? id.hashCode() : 0;
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        temp = Double.doubleToLongBits(weight);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
