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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.dockstore.webservice.core.Token;
import io.dockstore.webservice.core.TokenType;
import io.dockstore.webservice.core.User;
import io.dockstore.webservice.helpers.OidcHelper;
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
    private static final long MAX_DOCKER_TOKEN_LENGTH = 255; // max length of a docker token.
    private final TokenDAO tokenDAO;
    private final UserDAO userDAO;
    private final boolean autoRegister;

    SimpleAuthenticator(TokenDAO tokenDAO, UserDAO userDAO, Boolean autoRegisterUsers) {
        this.tokenDAO = tokenDAO;
        this.userDAO = userDAO;
        this.autoRegister = autoRegisterUsers;
    }

    private void updateToken(Token token, String tokenContent, DecodedJWT decodedJWT) {
        token.setContent(tokenContent);
        token.setRefreshToken(null);
        Instant expiresAt = decodedJWT.getExpiresAt().toInstant();
        token.setTokenExpiry(Timestamp.from(expiresAt));
        tokenDAO.update(token);
    }

    /**
     * Authenticates the credentials.
     * <p>
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

        Token token;
        //is the token a JWT from the configured OIDC provider?
        try {
            DecodedJWT decodedJWT = OidcHelper.verifyAndDecodeJwt(tokenContent);
            token = tokenDAO.findOidcBySubjectId(decodedJWT.getSubject());
            if (token != null) {
                updateToken(token, tokenContent, decodedJWT);
                User user = userDAO.findById(token.getUserId());
                if (user == null) {
                    return Optional.empty();
                }
                initializeUserProfiles(user);
                return Optional.of(user);
            } else {
                //Token is valid, but there is either (a) nothing linking it to the user, or (b) the user does not exist.
                User.Profile userProfile = OidcHelper.getUserProfile(tokenContent).orElse(null);
                if (userProfile == null) {
                    //couldn't get the needed user information from the token (via OIDC userinfo endpoint)
                    return Optional.empty();
                }

                User user = userDAO.findByOidcEmail(userProfile.email);
                long userId;
                if (user == null) {
                    if (!autoRegister) {
                        return Optional.empty();
                    }
                    user = new User();
                    user.setUsername(userProfile.email);
                    userId = userDAO.create(user);
                } else {
                    userId = user.getId();
                }

                initializeUserProfiles(user);
                // CREATE GOOGLE TOKEN
                Token oidcToken = new Token(tokenContent, null, userId, decodedJWT.getSubject(), TokenType.OIDC);
                tokenDAO.create(oidcToken);
                // Update user profile too
                user = userDAO.findById(userId);
                user.setAvatarUrl(userProfile.avatarURL);
                Map<String, User.Profile> userProfiles = user.getUserProfiles();
                userProfiles.put(TokenType.OIDC.toString(), userProfile);
                return Optional.of(user);
            }
        } catch (JWTDecodeException jde) {
            //token might still be a valid dockstore token.
            if (tokenContent.length() > MAX_DOCKER_TOKEN_LENGTH) {
                //Dockstore tokens are short.  If the token is too long, it's not a valid token.
                return Optional.empty();
            }
            token = tokenDAO.findByContent(tokenContent);
            User byId = userDAO.findById(token.getUserId());
            if (byId.isBanned()) {
                return Optional.empty();
            }
            initializeUserProfiles(byId);
            return Optional.of(byId);

        } catch (TokenExpiredException | IllegalArgumentException ex) {
            return Optional.empty();
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
