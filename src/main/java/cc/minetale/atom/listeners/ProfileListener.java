package cc.minetale.atom.listeners;

import cc.minetale.atom.Atom;
import cc.minetale.commonlib.modules.pigeon.payloads.profile.ProfileCreatePayload;
import cc.minetale.commonlib.modules.pigeon.payloads.profile.ProfileRequestPayload;
import cc.minetale.commonlib.modules.pigeon.payloads.profile.ProfileUpdatePayload;
import cc.minetale.commonlib.modules.profile.Profile;
import cc.minetale.commonlib.modules.profile.ProfileQueryResult;
import cc.minetale.pigeon.annotations.PayloadHandler;
import cc.minetale.pigeon.annotations.PayloadListener;
import cc.minetale.pigeon.feedback.RequiredState;
import cc.minetale.pigeon.listeners.Listener;

import java.util.Collections;

@PayloadListener
public class ProfileListener implements Listener {

    @PayloadHandler(requiredState = RequiredState.REQUEST)
    public void onProfileCreate(ProfileCreatePayload payload) {
        final var profilesManager = Atom.getAtom().getProfilesManager();

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
        final var profilesManager = Atom.getAtom().getProfilesManager();

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
        final var profilesManager = Atom.getAtom().getProfilesManager();

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
