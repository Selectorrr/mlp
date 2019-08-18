package net.org.selector.mlp.domain;


import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Created by Stepan Litvinov on 2019-08-18.
 */
@Value
@AllArgsConstructor
public class Node {
    private String value;
    private NodeType type;

    public enum NodeType {
        INPUT, OUTPUT, HIDDEN
    }

}
