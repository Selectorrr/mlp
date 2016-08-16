package net.org.selector;

import com.google.common.collect.ImmutableList;
import net.org.selector.mlp.dto.RequestDto;
import net.org.selector.mlp.services.MlpService;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.*;
import java.util.stream.Stream;

@SpringBootApplication
@Configuration
@EnableNeo4jRepositories(basePackages = "net.org.selector.mlp.repository")
@EnableTransactionManagement
@Controller
public class MlpApplication extends Neo4jConfiguration {
    @Autowired
    private MlpService mlpService;

    public static void main(String[] args) {
        SpringApplication.run(MlpApplication.class, args);
    }

    @RequestMapping("/train")
    @ResponseBody void train(@RequestBody RequestDto requestDto) {
        mlpService.trainQuery(requestDto.inputs, requestDto.outputs, requestDto.selected);
    }

    @RequestMapping("/sort")
    @ResponseBody
    List<String> sort(@RequestBody RequestDto requestDto) {
        double[] ranks = mlpService.getRanksForOutputs(ImmutableList.copyOf(requestDto.inputs), ImmutableList.copyOf(requestDto.outputs));
        Map<String, Double> outputByRank = new HashMap<>();
        for (int i = 0; i < ranks.length; i++) {
            outputByRank.put(requestDto.outputs.get(i), ranks[i]);
        }
        Map<String, Double> sortedOutputsByRank = sortByValue(outputByRank);
        return ImmutableList.copyOf(sortedOutputsByRank.keySet()).reverse();
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {

        Map<K, V> result = new LinkedHashMap<>();
        Stream<Map.Entry<K, V>> st = map.entrySet().stream();
        st.sorted(Map.Entry.comparingByValue()).forEachOrdered(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    @Bean
    public FilterRegistrationBean corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        source.registerCorsConfiguration("/**", config);
        // return new CorsFilter(source);
        final FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }

    @Bean
    public org.neo4j.ogm.config.Configuration getConfiguration() {
        org.neo4j.ogm.config.Configuration config = new org.neo4j.ogm.config.Configuration();
        config
                .driverConfiguration()
                .setDriverClassName("org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver")
                .setURI("file:///brain.db");
        return config;
    }

    @Override
    @Bean
    public SessionFactory getSessionFactory() {
        return new SessionFactory(getConfiguration(), "net.org.selector.mlp.domain");
    }

//    @Bean
//    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
//    public Session getSession() throws Exception {
//        return super.getSession();
//    }
}
