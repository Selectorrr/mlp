package net.org.selector.mlp.repository;

import net.org.selector.mlp.domain.Synapse;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.List;

/**
 * Created by Selector on 21.07.2016.
 */
public interface SynapseRepository extends GraphRepository<Synapse> {
    @Query("MATCH (from)-[r]-(to) WHERE ID(from)={0} AND ID(to)={1} RETURN r")
    Synapse getBetween(Long from, Long to);

    @Query("MATCH (from)-[r]-(to) WHERE ID(from) in {0} AND ID(to) in {1} RETURN r")
    List<Synapse> getBetween(List<Long> from, List<Long> to);


}
