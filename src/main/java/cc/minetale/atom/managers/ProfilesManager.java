package cc.minetale.atom.managers;

import cc.minetale.commonlib.modules.profile.Profile;
import cc.minetale.commonlib.modules.profile.ProfileQueryResult;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ProfilesManager {

    private Cache<UUID, Profile> cache;

    public ProfilesManager(CacheManager cacheManager) {
        this.cache = cacheManager.createCache("profiles",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(UUID.class, Profile.class,
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
        this.cache.put(profile.getId(), profile);
        var result = Profile.getCollection().replaceOne(Filters.eq(profile.getId().toString()), profile.toDocument(), new ReplaceOptions().upsert(true));
    }

    public CompletableFuture<Profile> getProfile(String name) {
        return new CompletableFuture<Profile>()
                .completeAsync(() -> {
                    for(Cache.Entry<UUID, Profile> ent : this.cache) {
                        var profile = ent.getValue();
                        if(profile.getName().equalsIgnoreCase(name))
                            return profile;
                    }

                    var document = Profile.getCollection().find(Filters.eq("searchableName", name.toUpperCase())).first();
                    if(document == null) { return null; }

                    var profile = Profile.fromDocument(document);
                    cache.put(profile.getId(), profile);

                    return profile;
                });
    }

    public CompletableFuture<Profile> getProfile(UUID uuid) {
        return new CompletableFuture<Profile>()
                .completeAsync(() -> {
                    var profile = this.cache.get(uuid);
                    if(profile != null) { return profile; }

                    var document = Profile.getCollection().find(Filters.eq(uuid.toString())).first();
                    if(document == null) { return null; }

                    profile = Profile.fromDocument(document);
                    cache.put(profile.getId(), profile);

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

                    for(Cache.Entry<UUID, Profile> ent : this.cache) {
                        var profile = ent.getValue();
                        for(final var it = searchable.iterator(); it.hasNext();) {
                            var name = it.next();
                            if(name == null || name.isEmpty()) { continue; }

                            if(profile.getName().equals(name)) {
                                it.remove();
                                profiles.add(profile);
                            }
                        }
                    }

                    var documents = Profile.getCollection().find(Filters.in("searchableName", searchable));

                    try(MongoCursor<Document> cursor = documents.cursor()) {
                        while(cursor.hasNext()) {
                            var document = cursor.next();

                            var profile = Profile.fromDocument(document);
                            cache.put(profile.getId(), profile);
                        }
                    }

                    return profiles;
                });
    }

    public CompletableFuture<List<Profile>> getProfilesByIds(List<UUID> ids) {
        return new CompletableFuture<List<Profile>>()
                .completeAsync(() -> {
                    List<Profile> profiles = new ArrayList<>();

                    for(Cache.Entry<UUID, Profile> ent : this.cache) {
                        var profile = ent.getValue();
                        for(final var it = ids.iterator(); it.hasNext();) {
                            var id = it.next();
                            if(id == null) { continue; }

                            if(profile.getId().equals(id)) {
                                it.remove();
                                profiles.add(profile);
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

                            var profile = Profile.fromDocument(document);
                            cache.put(profile.getId(), profile);
                        }
                    }

                    return profiles;
                });
    }

}
