package cc.minetale.atom.managers;

import cc.minetale.atom.network.Player;
import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.profile.ProfileQueryResult;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import lombok.Getter;
import org.bson.Document;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
public class PlayerManager {

    private final Cache<UUID, Player> cache;

    public PlayerManager(CacheManager cacheManager) {
        this.cache = cacheManager.createCache("players",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(UUID.class, Player.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().heap(100000, EntryUnit.ENTRIES)));
    }

    public CompletableFuture<ProfileQueryResult> createProfile(String name, UUID uuid) {
        return createProfile(new Profile(name, uuid));
    }

    public CompletableFuture<ProfileQueryResult> createProfile(Profile profile) {
        return new CompletableFuture<ProfileQueryResult>()
                .completeAsync(() -> {
                    try {
                        if(getProfile(profile.getId()).get() != null) { return ProfileQueryResult.PROFILE_EXISTS; }
                        updateProfile(profile);
                        return ProfileQueryResult.CREATED_PROFILE;
                    } catch(InterruptedException | ExecutionException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                    return ProfileQueryResult.FAILURE;
                });
    }

    public void updateProfile(Profile profile) {
        Player player = this.cache.get(profile.getId());

        if(player != null) {
            player.setProfile(profile);
        } else {
            this.cache.put(profile.getId(), new Player(profile));
        }

        Profile.getCollection().replaceOne(Filters.eq(profile.getId().toString()), profile.toDocument(), new ReplaceOptions().upsert(true));
    }

    public CompletableFuture<Profile> getProfile(String name) {
        return new CompletableFuture<Profile>()
                .completeAsync(() -> {
                    for(Cache.Entry<UUID, Player> ent : this.cache) {
                        Player player = ent.getValue();
                        if(player.getName().equalsIgnoreCase(name))
                            return player.getProfile();
                    }

                    var document = Profile.getCollection().find(Filters.eq("searchableName", name.toUpperCase())).first();
                    if(document == null) { return null; }

                    Profile profile = Profile.fromDocument(document);
                    Player player = this.cache.get(profile.getId());

                    if(player != null) {
                        player.setProfile(profile);
                    } else {
                        this.cache.put(profile.getId(), new Player(profile));
                    }

                    return profile;
                });
    }

    public CompletableFuture<Profile> getProfile(UUID uuid) {
        return new CompletableFuture<Profile>()
                .completeAsync(() -> {
                    Player player = this.cache.get(uuid);
                    if(player != null) { return player.getProfile(); }

                    var document = Profile.getCollection().find(Filters.eq(uuid.toString())).first();
                    if(document == null) { return null; }

                    Profile profile = Profile.fromDocument(document);
                    player = this.cache.get(profile.getId());

                    if(player != null) {
                        player.setProfile(profile);
                    } else {
                        this.cache.put(profile.getId(), new Player(profile));
                    }

                    return profile;
                });
    }

    public CompletableFuture<List<Profile>> getProfilesByNames(List<String> names) {
        return new CompletableFuture<List<Profile>>()
                .completeAsync(() -> {
                    List<Profile> profiles = new ArrayList<>();

                    List<String> searchable = names.stream()
                            .map(String::toUpperCase)
                            .collect(Collectors.toList());

                    for(Cache.Entry<UUID, Player> ent : this.cache) {
                        Player player = ent.getValue();
                        for(final var it = searchable.iterator(); it.hasNext();) {
                            var name = it.next();
                            if(name == null || name.isEmpty()) { continue; }

                            if(player.getName().equals(name)) {
                                it.remove();
                                profiles.add(player.getProfile());
                            }
                        }
                    }

                    var documents = Profile.getCollection().find(Filters.in("searchableName", searchable));

                    try(MongoCursor<Document> cursor = documents.cursor()) {
                        while(cursor.hasNext()) {
                            var document = cursor.next();

                            var profile = Profile.fromDocument(document);
                            Player player = this.cache.get(profile.getId());

                            if(player != null) {
                                player.setProfile(profile);
                            } else {
                                this.cache.put(profile.getId(), new Player(profile));
                            }
                        }
                    }

                    return profiles;
                });
    }

    public CompletableFuture<List<Profile>> getProfilesByIds(List<UUID> ids) {
        return new CompletableFuture<List<Profile>>()
                .completeAsync(() -> {
                    List<Profile> profiles = new ArrayList<>();

                    for(Cache.Entry<UUID, Player> ent : this.cache) {
                        Player player = ent.getValue();
                        for(final var it = ids.iterator(); it.hasNext();) {
                            var id = it.next();
                            if(id == null) { continue; }

                            if(player.getUuid().equals(id)) {
                                it.remove();
                                profiles.add(player.getProfile());
                            }
                        }
                    }

                    List<String> searchable = ids.stream()
                            .map(UUID::toString)
                            .collect(Collectors.toList());

                    var documents = Profile.getCollection().find(Filters.in("_id", searchable));

                    try(MongoCursor<Document> cursor = documents.cursor()) {
                        while(cursor.hasNext()) {
                            var document = cursor.next();

                            Profile profile = Profile.fromDocument(document);
                            Player player = this.cache.get(profile.getId());

                            if(player != null) {
                                player.setProfile(profile);
                            } else {
                                this.cache.put(profile.getId(), new Player(profile));
                            }
                        }
                    }

                    return profiles;
                });
    }

}
