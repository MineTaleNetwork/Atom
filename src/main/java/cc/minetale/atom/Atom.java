package cc.minetale.atom;

import cc.minetale.atom.listeners.*;
import cc.minetale.atom.managers.ProfilesManager;
import cc.minetale.atom.util.Logger;
import cc.minetale.atom.util.timer.TimerManager;
import cc.minetale.commonlib.CommonLib;
import cc.minetale.commonlib.modules.network.Server;
import cc.minetale.pigeon.Pigeon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;

import java.util.Arrays;

@Getter
public class Atom {

    // TODO: PERMS (CTRL SHIFT F)
    // TODO: Friend Requests
    // TODO: Cooldowns
    // TODO: Gamemode listener

    @Getter
    private static Atom atom;

    private final Gson gson;

    private final TimerManager timerManager;

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

    private Pigeon pigeon;

    private CacheManager cacheManager;

    private ProfilesManager profilesManager;

    public Atom() {
        long start = System.currentTimeMillis();

        Logger.log(Logger.Level.INFO, "Starting the initialization process of Atom.");

        atom = this;

        this.gson = new GsonBuilder().create();

        this.timerManager = new TimerManager();

        new Thread(() -> {
            while (true) {
                try {
                    Server.serverList.removeIf(server -> (System.currentTimeMillis() - server.getLastUpdated()) >= (1000 * 30));
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        loadMongo();

        this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
        this.profilesManager = new ProfilesManager(this.cacheManager);

        loadPigeon();

        new CommonLib(this.mongoClient, this.mongoDatabase, this.pigeon);

        Logger.log(Logger.Level.INFO, "Done (" + (System.currentTimeMillis() - start) + "ms)! Atom has successfully been initialized!");
    }

    private void loadMongo() {
        this.mongoClient = new MongoClient("127.0.0.1", 27017);
        this.mongoDatabase = mongoClient.getDatabase("MineTale");
    }

    private void loadPigeon() {
        this.pigeon = new Pigeon();

        this.pigeon.initialize("127.0.0.1", 5672, "minetale", "atom");

        Arrays.asList(
                new ConversationListener(),
                new FriendListener(),
                new PartyListener(),
                new ProfileListener(),
                new ProxyListener(),
                new RankListener()
        ).forEach(listener -> this.pigeon.getListenersRegistry().registerListener(listener));

        this.pigeon.setupDefaultUpdater();

        this.pigeon.acceptDelivery();
    }

}
