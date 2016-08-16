package net.org.selector.mlp.dto;

import java.util.List;

public class RequestDto {
    public List<String> inputs;
    public List<String> outputs;
    public String selected;

    public RequestDto() {
    }

    public RequestDto(List<String> inputs, List<String> outputs, String selected) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.selected = selected;
    }
}
