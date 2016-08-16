package net.org.selector.mlp.repository;

import net.org.selector.mlp.domain.Neuron;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.List;

/**
 * Created by Selector on 21.07.2016.
 */
public interface NeuronRepository extends GraphRepository<Neuron> {
    Neuron findOneByTypeAndValue(String type, String value);

    @Query("MATCH (i)-[r]-(h)-[r2]-(o) WHERE (i.value in {0} and i.type = 'IN') AND (o.value in {1} AND o.type = 'OUT') RETURN h")
    List<Neuron> getHiddenNodesBetween(List<String> inputs, List<String> outputs);

    @Query("MATCH (n) WHERE (n.value in {1} and n.type = {0}) RETURN n")
    List<Neuron> getByTypeAndValueIn(String type, List<String> inputVals);
}
