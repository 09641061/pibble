package com.takodu.api.iam.interfaces.acl;

import java.util.Optional;
import java.util.UUID;

/** What other contexts may ask iam. Users belong to no organization, so no method takes one. */
public interface IamContextFacade {

    /**
     * @param userId the user, usually the subject of the caller's access token
     * @return the user's normalized email, empty when no user has this id
     */
    Optional<String> fetchUserEmail(UUID userId);
}
