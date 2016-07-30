package net.org.selector.model.mlp;

import org.neo4j.ogm.annotation.NodeEntity;

/**
 * Created by Selector on 29.07.2016.
 */
@NodeEntity
public class Neuron {
    public static final String TYPE_IN = "IN";
    public static final String TYPE_HIDDEN = "HIDDEN";
    public static final String TYPE_OUT = "OUT";
    private Long id;
    private String type;
    private String value;

    public Neuron() {
    }

    public Neuron(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Neuron neuron = (Neuron) o;

        if (id != null ? !id.equals(neuron.id) : neuron.id != null) return false;
        if (type != null ? !type.equals(neuron.type) : neuron.type != null) return false;
        return value != null ? value.equals(neuron.value) : neuron.value == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
