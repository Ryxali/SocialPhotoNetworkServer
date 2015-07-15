package com.speedment.examples.socialserver;

import com.company.speedment.test.socialnetwork.SocialnetworkApplication;
import com.company.speedment.test.socialnetwork.db0.socialnetwork.image.Image;
import com.company.speedment.test.socialnetwork.db0.socialnetwork.image.ImageField;
import com.company.speedment.test.socialnetwork.db0.socialnetwork.image.ImageManager;
import com.company.speedment.test.socialnetwork.db0.socialnetwork.user.User;
import com.company.speedment.test.socialnetwork.db0.socialnetwork.user.UserField;
import com.company.speedment.test.socialnetwork.db0.socialnetwork.user.UserManager;
import com.speedment.util.json.Json;
import fi.iki.elonen.ServerRunner;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import static java.util.stream.Collectors.joining;
import java.util.stream.Stream;

/**
 *
 * @author Emil Forslund
 */
public class Server extends ServerBase {

    protected final Random random = new SecureRandom();
    private Map<String, Long> sessionKeys = new HashMap<String, Long>();

    public Server() {
        new SocialnetworkApplication().start();
    }

    private String createSession(User user) {
        final String key = nextSessionId();
        sessionKeys.put(key, user.getId());
        return key;
    }

    private Optional<User> getLoggedIn(String key) {
        return Optional.ofNullable(sessionKeys.get(key))
                .flatMap(id -> User.stream()
                        .filter(UserField.ID.equal(id)).findAny()
                );
    }

    @Override
    public String onRegister(String mail, String password) {
        // TODO: Write register function.
        String ret = User.builder()
                .setMail(mail)
                .setPassword(password)
                .persist()
                .map(this::createSession)
                .orElse("false");
        return ret;
    }

    @Override
    public String onLogin(String mail, String password) {
        // TODO: Write login function.
        String ret = User.stream()
                .filter(UserField.MAIL.equalIgnoreCase(mail))
                .filter(UserField.PASSWORD.equal(password))
                .findAny()
                .map(this::createSession)
                .orElse("false");
        return ret;
    }

    @Override
    public String onSelf(String sessionKey) {
        // TODO: Write self function.
        return getLoggedIn(sessionKey)
                .map(u -> Json.allFrom(UserManager.get())
                        .remove(UserField.PASSWORD)
                        .remove(UserField.FIRSTNAME)
                        .build(u)
                )
                .orElse("false");
    }

    @Override
    public String onUpload(String title, String description, String imgData, String sessionKey) {
        return getLoggedIn(sessionKey).map(
                u
                -> Image.builder()
                .setTitle(title)
                .setDescription(description)
                .setImgData(imgData)
                .setUploader(u.getId())
                .persist()
        ).map(i -> "true").orElse("false");

    }

    @Override
    public String onFind(String freeText, String sessionKey) {
        // TODO: Write find function.
        return "false";
    }

    @Override
    public String onFollow(long userId, String sessionKey) {
        // TODO: Write follow function.
        return "false";
    }

    @Override
    public String onBrowse(String sessionKey, Optional<Timestamp> from, Optional<Timestamp> to) {
        // TODO: Write browse function.
        /*return Image.stream()
         //.filter(ImageField.UPLOADED.greaterOrEqual(from.orElse(Timestamp.from(Instant.EPOCH))))
         //.filter(ImageField.UPLOADED.lessOrEqual(to.orElse(Timestamp.from(Instant.MAX))))
         .findAny()
         .map(i -> Json.allFrom(ImageManager.get())
         .put("uploader", iu -> iu.findUploader().toJson())
         .build(i))
         .orElse("false");*/
        return getLoggedIn(sessionKey).map(me
                -> "{\"images\":["
                + Stream.concat(
                        Stream.of(me),
                        me.linksByFollower()
                        .map(link -> link.findFollows())
                )
                .flatMap(User::images)
                        
                        
                .map(img -> Json.allFrom(ImageManager.get())
                        .put(ImageField.UPLOADER,
                                Json.allFrom(UserManager.get())
                                .remove(UserField.AVATAR)
                                .remove(UserField.PASSWORD)
                        ).build(img)
                ).collect(joining(","))
                + "]}"
        ).orElse("false");
    }

    @Override
    public String onUpdate(String mail, String firstname, String lastName, Optional<String> avatar, String sessionKey) {
        // TODO: Write update profile
        return "false";
    }

    protected String nextSessionId() {
        return new BigInteger(130, random).toString(32);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        ServerRunner.run(Server.class);
    }
}
