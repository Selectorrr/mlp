package net.org.selector.mlp.services;

import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import net.org.selector.mlp.MlpApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

/**
 * Created by Stepan Litvinov on 2019-08-18.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MlpApplication.class)
@Slf4j
public class MlpServiceTest {

    private final int MAX_USERS = 150;
    private final int MAX_LIKES = 15;
    @Autowired
    private MlpService mlpService;
    private ImmutableList<String> users;
    private Multimap<String, String> likesPerUser;
    private Map<String, Multimap<String, String>> userLikePrev;

    @Before
    public void setUp() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (int i = 0; i < MAX_USERS + 1; i++) {
            builder.add(String.valueOf(i));
        }
        users = builder.build();


        likesPerUser = HashMultimap.create();

        users.forEach(curUser -> {
            List<String> otherUsers = new ArrayList<>(users);
            otherUsers.remove(curUser);
            while (likesPerUser.get(curUser).size() < MAX_LIKES) {
                int random = new Random().nextInt(otherUsers.size());
                String randomUser = otherUsers.get(random);
                likesPerUser.put(curUser, randomUser);
            }
        });


        userLikePrev = new HashMap<>();

        likesPerUser.keySet().forEach(user -> {
            Collection<String> likes = likesPerUser.get(user);

            List<String> otherUsers = new ArrayList<>(users);
            otherUsers.remove(user);

            Multimap<String, String> behave = HashMultimap.create();

            List<String> buffer = new ArrayList<>();
            otherUsers.forEach(otherUser -> {
                buffer.add(otherUser);
                if (likes.contains(otherUser)) {
                    behave.putAll(otherUser, buffer);
                    buffer.clear();
                }
            });
            userLikePrev.put(user, behave);
        });
    }

    @Test
    public void work() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        userLikePrev.keySet()
                .stream()
                .parallel()
                .forEach(curUser -> {
                    Multimap<String, String> likePrev = userLikePrev.get(curUser);

                    List<String> likes = new ArrayList<>();

                    likePrev.keySet().forEach(like -> {
                        Collection<String> shown = likePrev.get(like);
                        likes.add(like);
                        mlpService.trainQuery(likes, new ArrayList<>(shown), like);


                    });
                });

        log.info("time: " + stopwatch);
    }


}
