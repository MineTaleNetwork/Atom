package cc.minetale.atom.listeners;

import cc.minetale.atom.Atom;
import cc.minetale.atom.network.FriendRequest;
import cc.minetale.atom.network.PartyInvite;
import cc.minetale.atom.network.Player;
import cc.minetale.commonlib.pigeon.payloads.profile.IgnorePlayerPayload;
import cc.minetale.commonlib.pigeon.payloads.profile.ProfileCreatePayload;
import cc.minetale.commonlib.pigeon.payloads.profile.ProfileRequestPayload;
import cc.minetale.commonlib.pigeon.payloads.profile.ProfileUpdatePayload;
import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.profile.ProfileQueryResult;
import cc.minetale.pigeon.annotations.PayloadHandler;
import cc.minetale.pigeon.annotations.PayloadListener;
import cc.minetale.pigeon.feedback.RequiredState;
import cc.minetale.pigeon.listeners.Listener;

import java.util.Collections;
import java.util.UUID;

@PayloadListener
public class ProfileListener implements Listener {

    @PayloadHandler
    public void onPlayerIgnore(IgnorePlayerPayload payload) {

        // TODO -> Check if they are already ignored
        // TODO -> Send a message back to the player on success
        // TODO -> Send proper messages (Player removed you from their friends list)

        var initiatorUUID = payload.getInitiator();
        var playerUUID = payload.getPlayer();

        FriendRequest.removeRequests(initiatorUUID, playerUUID);

        Player.getPlayer(initiatorUUID).thenAccept(initiator -> Player.getPlayer(playerUUID).thenAccept(player -> {
            initiator.getPartyInvites().remove(playerUUID);
            player.getPartyInvites().remove(initiatorUUID);

            if(initiator.getLastMessaged() != null && initiator.getLastMessaged().equals(player)) {
                initiator.setLastMessaged(null);
            }

            if(player.getLastMessaged() != null && player.getLastMessaged().equals(initiator)) {
                player.setLastMessaged(null);
            }

            var initiatorProfile = initiator.getProfile();
            var playerProfile = player.getProfile();

            initiatorProfile.getFriends().remove(playerUUID);
            playerProfile.getFriends().remove(initiatorUUID);
        }));
    }

    @PayloadHandler(requiredState = RequiredState.REQUEST)
    public void onProfileCreate(ProfileCreatePayload payload) {
        final var profilesManager = Atom.getAtom().getPlayerManager();

        var profile = payload.getProfile();
        var id = profile.getId();

        profilesManager
                .getProfile(id)
                .thenAccept(existingProfile -> {
                    profilesManager
                            .createProfile(profile)
                            .thenAccept(result -> payload.sendResponse(new ProfileCreatePayload(result)));
                });
    }

    @PayloadHandler(requiredState = RequiredState.REQUEST)
    public void onProfileRequest(final ProfileRequestPayload payload) {
        final var profilesManager = Atom.getAtom().getPlayerManager();

        var type = payload.getType();

        if(type == ProfileRequestPayload.Type.SINGLE) {
            var name = payload.getName();
            var id = payload.getId();

            if(id != null && name != null) {
                profilesManager
                        .getProfile(id)
                        .thenAccept(existingProfile -> {
                            if(existingProfile != null) {
                                payload.sendResponse(new ProfileRequestPayload(ProfileQueryResult.RETRIEVED, Collections.singletonList(existingProfile)));
                                return;
                            }

                            var profile = new Profile(name, id);
                            profilesManager
                                    .createProfile(profile)
                                    .thenAccept(result -> payload.sendResponse(new ProfileRequestPayload(result, Collections.singletonList(profile))));
                        });
            } else if(id != null) {
                profilesManager
                        .getProfile(id)
                        .thenAccept(profile -> {
                            if(profile != null) {
                                payload.sendResponse(new ProfileRequestPayload(
                                        ProfileQueryResult.RETRIEVED,
                                        Collections.singletonList(profile)));
                            } else {
                                payload.sendResponse(new ProfileRequestPayload(
                                        ProfileQueryResult.NOT_FOUND,
                                        Collections.emptyList()));
                            }
                        });
            } else if(name != null) {
                profilesManager
                        .getProfile(name)
                        .thenAccept(profile -> {
                            if(profile != null) {
                                payload.sendResponse(new ProfileRequestPayload(
                                        ProfileQueryResult.RETRIEVED,
                                        Collections.singletonList(profile)));
                            } else {
                                payload.sendResponse(new ProfileRequestPayload(
                                        ProfileQueryResult.NOT_FOUND,
                                        Collections.emptyList()));
                            }
                        });
            }
        } else if(type == ProfileRequestPayload.Type.BULK) {
            var names = payload.getNames();
            var ids = payload.getIds();

            if(payload.areConnected()) {
                if((ids != null && !ids.isEmpty()) && (names != null && !names.isEmpty())) {
                    var areConnected = payload.areConnected();
                    if(areConnected) {
                        final var nIt = names.iterator();
                        final var iIt = ids.iterator();
                        while(nIt.hasNext()) {
                            if(!iIt.hasNext()) { break; }

                            var name = nIt.next();
                            if(name == null || name.isEmpty()) { continue; }

                            var id = iIt.next();
                            if(id == null) { continue; }

                            var profile = new Profile(name, id);
                            profilesManager
                                    .getProfile(id)
                                    .thenAccept(existingProfile -> {
                                        if(existingProfile != null) {
                                            payload.sendResponse(new ProfileRequestPayload(ProfileQueryResult.RETRIEVED,
                                                    Collections.singletonList(existingProfile)));
                                            return;
                                        }

                                        profilesManager
                                                .createProfile(profile)
                                                .thenAccept(result -> payload.sendResponse(new ProfileRequestPayload(result,
                                                        Collections.singletonList(profile))));
                                    });
                        }
                    }
                }
            } else {
                if(ids != null && !ids.isEmpty()) {
                    profilesManager
                            .getProfilesByIds(ids)
                            .thenAccept(profiles -> {
                                if(profiles != null && !profiles.isEmpty()) {
                                    payload.sendResponse(new ProfileRequestPayload(
                                            ProfileQueryResult.RETRIEVED,
                                            profiles));
                                } else {
                                    payload.sendResponse(new ProfileRequestPayload(
                                            ProfileQueryResult.NOT_FOUND,
                                            Collections.emptyList()));
                                }
                            });
                }

                if(names != null && !names.isEmpty()) {
                    profilesManager
                            .getProfilesByNames(names)
                            .thenAccept(profiles -> {
                                if(profiles != null && !profiles.isEmpty()) {
                                    payload.sendResponse(new ProfileRequestPayload(
                                            ProfileQueryResult.RETRIEVED,
                                            profiles));
                                } else {
                                    payload.sendResponse(new ProfileRequestPayload(
                                            ProfileQueryResult.NOT_FOUND,
                                            Collections.emptyList()));
                                }
                            });
                }
            }
        }
    }

    @PayloadHandler(requiredState = RequiredState.REQUEST)
    public void onProfileUpdate(ProfileUpdatePayload payload) {
        final var profilesManager = Atom.getAtom().getPlayerManager();

        var profile = payload.getProfile();
        var id = profile.getId();

        profilesManager
                .getProfile(id)
                .thenAccept(existingProfile -> {
                    if(existingProfile == null) {
                        payload.sendResponse(new ProfileUpdatePayload(ProfileQueryResult.NOT_FOUND));
                        return;
                    }

                    profilesManager.updateProfile(profile);

                    payload.sendResponse(new ProfileUpdatePayload(ProfileQueryResult.UPDATED_PROFILE));
                });
    }

}
