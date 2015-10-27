/*
 * Copyright (C) 2015 Consonance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.dockstore.webservice.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import io.dockstore.webservice.Helper;
import io.dockstore.webservice.core.Token;
import io.dockstore.webservice.core.TokenType;
import io.dockstore.webservice.core.User;
import io.dockstore.webservice.jdbi.TokenDAO;
import io.dockstore.webservice.jdbi.UserDAO;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The token resource handles operations with tokens. Tokens are needed to talk with the quay.io and github APIs. In addition, they will be
 * needed to pull down docker containers that are requested by users.
 *
 * @author dyuen
 */
@Path("/auth/tokens")
@Api(value = "/auth/tokens", authorizations = { @Authorization(value = "dockstore_auth", scopes = { @AuthorizationScope(scope = "read:tokens", description = "read tokens") }) }, tags = "tokens")
@Produces(MediaType.APPLICATION_JSON)
public class TokenResource {
    private final TokenDAO tokenDAO;
    private final UserDAO userDAO;
    private static final String TARGET_URL = "https://github.com/";
    private static final String QUAY_URL = "https://quay.io/api/v1/";
    private final String githubClientID;
    private final String githubClientSecret;
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    private static final Logger LOG = LoggerFactory.getLogger(TokenResource.class);

    public TokenResource(ObjectMapper mapper, TokenDAO tokenDAO, UserDAO enduserDAO, String githubClientID, String githubClientSecret,
            HttpClient client) {
        this.objectMapper = mapper;
        this.tokenDAO = tokenDAO;
        this.userDAO = enduserDAO;
        this.githubClientID = githubClientID;
        this.githubClientSecret = githubClientSecret;
        this.client = client;
    }

    private static class QuayUser {
        private String username;

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUsername() {
            return this.username;
        }
    }

    @GET
    @Timed
    @UnitOfWork
    @ApiOperation(value = "List all known tokens", notes = "List all tokens. Admin Only.", response = Token.class, responseContainer = "List")
    public List<Token> listTokens(@ApiParam(hidden = true) @Auth Token authToken) {
        User user = userDAO.findById(authToken.getUserId());
        Helper.checkUser(user);

        return tokenDAO.findAll();
    }

    @GET
    @Path("/{tokenId}")
    @Timed
    @UnitOfWork
    @ApiOperation(value = "Get a specific token by id", notes = "Get a specific token by id", response = Token.class)
    @ApiResponses(value = { @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = "Invalid ID supplied"),
            @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = "Token not found") })
    public Token listToken(@ApiParam(hidden = true) @Auth Token authToken,
            @ApiParam(value = "ID of token to return") @PathParam("tokenId") Long tokenId) {
        User user = userDAO.findById(authToken.getUserId());
        Token t = tokenDAO.findById(tokenId);
        Helper.checkUser(user, t.getUserId());

        return t;
    }

    @GET
    @Timed
    @UnitOfWork
    @Path("/quay.io")
    @ApiOperation(value = "Add a new quay IO token", notes = "This is used as part of the OAuth 2 web flow. "
            + "Once a user has approved permissions for Collaboratory"
            + "Their browser will load the redirect URI which should resolve here", response = Token.class)
    public Token addQuayToken(@ApiParam(hidden = true) @Auth Token authToken, @QueryParam("access_token") String accessToken) {
        if (accessToken.isEmpty()) {
            throw new WebApplicationException(HttpStatus.SC_BAD_REQUEST);
        }

        User user = userDAO.findById(authToken.getUserId());

        String url = QUAY_URL + "user/";
        Optional<String> asString = ResourceUtilities.asString(url, accessToken, client);

        String username = null;
        if (asString.isPresent()) {
            LOG.info("RESOURCE CALL: " + url);

            String response = asString.get();
            Gson gson = new Gson();
            Map<String, String> map = new HashMap<>();
            map = (Map<String, String>) gson.fromJson(response, map.getClass());

            username = map.get("username");
            LOG.info("Username: " + username);
        }

        Token token = new Token();
        token.setTokenSource(TokenType.QUAY_IO.toString());
        token.setContent(accessToken);

        if (user != null) {
            token.setUserId(user.getId());
        }

        if (username != null) {
            token.setUsername(username);
        } else {
            LOG.info("Quay.io tokenusername is null, did not create token");
            throw new WebApplicationException("Username not found from resource call " + url);
        }

        long create = tokenDAO.create(token);
        return tokenDAO.findById(create);
    }

    @DELETE
    @Path("/{tokenId}")
    @UnitOfWork
    @ApiOperation(value = "Deletes a token")
    @ApiResponses(value = { @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = "Invalid token value") })
    public Response deleteToken(@ApiParam(hidden = true) @Auth Token authToken,
            @ApiParam(value = "Token id to delete", required = true) @PathParam("tokenId") Long tokenId) {
        User user = userDAO.findById(authToken.getUserId());
        Token token = tokenDAO.findById(tokenId);
        Helper.checkUser(user, token.getUserId());

        tokenDAO.delete(token);

        token = tokenDAO.findById(tokenId);
        if (token == null) {
            return Response.ok().build();
        } else {
            return Response.serverError().build();
        }
    }

    @GET
    @Timed
    @UnitOfWork
    @Path("/github.com")
    @ApiOperation(value = "Add a new github.com token, used by quay.io redirect", notes = "This is used as part of the OAuth 2 web flow. "
            + "Once a user has approved permissions for Collaboratory"
            + "Their browser will load the redirect URI which should resolve here", response = Token.class)
    public Token addGithubToken(@QueryParam("code") String code) {
        Optional<String> asString = ResourceUtilities.asString(TARGET_URL + "login/oauth/access_token?code=" + code + "&client_id="
                + githubClientID + "&client_secret=" + githubClientSecret, null, client);
        String accessToken;
        if (asString.isPresent()) {
            Map<String, String> split = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(asString.get());
            accessToken = split.get("access_token");
        } else {
            throw new WebApplicationException("Could not retrieve github.com token based on code");
        }

        GitHubClient githubClient = new GitHubClient();
        githubClient.setOAuth2Token(accessToken);
        long userID = 0;
        String githubLogin;
        Token dockstoreToken;
        try {
            UserService uService = new UserService(githubClient);
            org.eclipse.egit.github.core.User githubUser = uService.getUser();

            githubLogin = githubUser.getLogin();
        } catch (IOException ex) {
            throw new WebApplicationException("Token ignored due to IOException");
        }

        User user = userDAO.findByUsername(githubLogin);
        if (user == null) {
            user = new User();
            user.setUsername(githubLogin);
            userID = userDAO.create(user);

            // CREATE DOCKSTORE TOKEN
            final Random random = new Random();
            final int bufferLength = 1024;
            final byte[] buffer = new byte[bufferLength];
            random.nextBytes(buffer);
            String randomString = BaseEncoding.base64Url().omitPadding().encode(buffer);
            final String dockstoreAccessToken = Hashing.sha256().hashString(githubLogin + randomString, Charsets.UTF_8).toString();

            dockstoreToken = new Token();
            dockstoreToken.setTokenSource(TokenType.DOCKSTORE.toString());
            dockstoreToken.setContent(dockstoreAccessToken);
            dockstoreToken.setUserId(userID);
            dockstoreToken.setUsername(githubLogin);
            long dockstoreTokenId = tokenDAO.create(dockstoreToken);
            dockstoreToken = tokenDAO.findById(dockstoreTokenId);

        } else {
            userID = user.getId();
            dockstoreToken = tokenDAO.findDockstoreByUserId(userID);
        }

        // CREATE GITHUB TOKEN
        Token token = new Token();
        token.setTokenSource(TokenType.GITHUB_COM.toString());
        token.setContent(accessToken);
        token.setUserId(userID);
        token.setUsername(githubLogin);
        tokenDAO.create(token);

        return dockstoreToken;
    }
}