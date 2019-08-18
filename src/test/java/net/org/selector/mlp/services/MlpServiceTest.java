package net.org.selector.mlp.services;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.org.selector.mlp.MlpApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Stepan Litvinov on 2019-08-18.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = MlpApplication.class)
@Slf4j
public class MlpServiceTest {

    private final int MAX_USERS = 300;
    private final int MAX_LIKES = 30;
    @Autowired
    private MlpService mlpService;
    private ImmutableList<String> users;

    @Before
    public void setUp() {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (int i = 0; i < MAX_USERS + 1; i++) {
            builder.add(String.valueOf(i));
        }
        users = builder.build();
    }

    @Test
    public void test() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        users.forEach(curUser -> {
            List<String> likes = new ArrayList<>();
            List<String> otherUsers = new ArrayList<>(users);
            otherUsers.remove(curUser);

            for (int i = 0; i < MAX_LIKES + 1; i++) {

                int random = new Random().nextInt(otherUsers.size());
                String randomUser = otherUsers.get(random);

//                log.info("user: {} like another user: {}", curUser, randomUser);
                mlpService.trainQuery(likes, otherUsers, randomUser);
                otherUsers.remove(random);
                likes.add(curUser);

            }
        });
//        long millis = stopwatch.elapsed(MILLISECONDS);

        log.info("time: " + stopwatch);

    }


}