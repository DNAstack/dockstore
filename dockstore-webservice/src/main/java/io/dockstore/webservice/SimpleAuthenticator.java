/*
 *    Copyright 2017 OICR
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.dockstore.webservice;

import java.util.Optional;

import io.dockstore.webservice.core.Token;
import io.dockstore.webservice.core.User;
import io.dockstore.webservice.jdbi.TokenDAO;
import io.dockstore.webservice.jdbi.UserDAO;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.hibernate.UnitOfWork;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xliu
 */
public class SimpleAuthenticator implements Authenticator<String, User> {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleAuthenticator.class);

    private final TokenDAO dao;
    private final UserDAO userDAO;

    SimpleAuthenticator(TokenDAO dao, UserDAO userDAO) {
        this.dao = dao;
        this.userDAO = userDAO;
    }

    /**
     * Authenticates the credentials.
     *
     * Valid credentials can either be a Dockstore token or a Google access token, if the Google access token
     * is issued against a whitelisted Google client id.
     *
     * @param tokenContent
     * @return an optional user
     */
    @UnitOfWork
    @Override
    public Optional<User> authenticate(String tokenContent) {
        LOG.debug("SimpleAuthenticator called with {}", tokenContent);
        final Token token = dao.findByContent(tokenContent);
        if (token != null) { // It's a valid Dockstore token
            User byId = userDAO.findById(token.getUserId());
            if (byId.isBanned()) {
                return Optional.empty();
            }
            initializeUserProfiles(byId);
            return Optional.of(byId);
        } else { // It might be an OIDC access token
            LOG.warn("Could not locate token record corresponding to content " + tokenContent);
            return Optional.empty();
            //            return OidcHelper.getUserProfile(credentials)
            //                    .map(userProfile -> {
            //                        final String email = userProfile.email;
            //                        User user = userDAO.findByGoogleEmail(email);
            //                        if (user == null) {
            //                            user = createUser(userProfile);
            //                        }
            //                        user.setTemporaryCredential(credentials);
            //                        initializeUserProfiles(user);
            //                        return Optional.of(user);
            //                    }).filter(user -> !user.get().isBanned())
            //                    .orElse(Optional.empty());
            //
            //            return userinfoPlusFromToken(credentials)
            //                    .map(userinfoPlus -> {
            //                        final String email = userinfoPlus.getEmail();
            //                        User user = userDAO.findByGoogleEmail(email);
            //                        if (user == null) {
            //                            user = createUser(userinfoPlus);
            //                        }
            //                        user.setTemporaryCredential(credentials);
            //                        initializeUserProfiles(user);
            //                        return Optional.of(user);
            //                    }).filter(user -> !user.get().isBanned())
            //                    .orElse(Optional.empty());
        }
    }

    void initializeUserProfiles(User user) {
        // Always eagerly load yourself (your User object)
        Hibernate.initialize(user.getUserProfiles());
    }

    //    Optional<Userinfoplus> userinfoPlusFromToken(String credentials) {
    //        return GoogleHelper.userinfoplusFromToken(credentials);
    //    }

    //    User createUser(User.Profile userProfile) {
    //        User user = new User();
    //        user.setAvatarUrl(userProfile.avatarURL);
    //        Map<String, User.Profile> userProfiles = user.getUserProfiles();
    //        userProfiles.put(TokenType.OIDC.toString(), userProfile);
    //        return user;
    //
    //        user.setAvatarUrl(userinfo.getPicture());
    //        Map<String, User.Profile> userProfile = user.getUserProfiles();
    //        userProfile.put(TokenType.OIDC.toString(), profile);
    //        User user = new User();
    //        user.setAvatarUrl(userinfo.getPicture());
    //        GoogleHelper.updateUserFromGoogleUserinfoplus(userinfoPlus, user);
    //        user.setUsername(userinfoPlus.getEmail());
    //        return user;
    //    }

}
