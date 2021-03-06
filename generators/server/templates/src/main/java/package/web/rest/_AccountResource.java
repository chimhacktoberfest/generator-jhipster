<%#
 Copyright 2013-2017 the original author or authors from the JHipster project.

 This file is part of the JHipster project, see http://www.jhipster.tech/
 for more information.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-%>
package <%=packageName%>.web.rest;

<%_ if (authenticationType === 'oauth2' && applicationType === 'gateway') { _%>

import com.codahale.metrics.annotation.Timed;
import com.mycompany.myapp.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/authenticate")
    @Timed
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET  /account : get the current user.
     *
     * @return the current user
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be returned
     */
    @GetMapping("/account")
    @Timed
    @SuppressWarnings("unchecked")
    public ResponseEntity<User> getAccount(Principal principal) {
        return Optional.ofNullable(principal)
            .filter(it -> it instanceof OAuth2Authentication)
            .map(it -> ((OAuth2Authentication) it).getUserAuthentication())
            .map(authentication -> {
                    Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
                    return new User(
                        authentication.getName(),
                        (String) details.get("given_name"),
                        (String) details.get("family_name"),
                        (String) details.get("email"),
                        (String) details.get("langKey"),
                        (String) details.get("imageUrl"),
                        (Boolean) details.get("email_verified"),
                        authentication.getAuthorities().stream().map(it -> it.getAuthority()).collect(Collectors.toSet())
                    );
                }
            )
            .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
    }

}
<%_ } else { _%>
import com.codahale.metrics.annotation.Timed;

<%_ if (authenticationType === 'session') { _%>
import <%=packageName%>.domain.PersistentToken;
<%_ } _%>
<%_ if (authenticationType === 'oauth2') { _%>
import <%=packageName%>.domain.Authority;
<%_ } _%>
import <%=packageName%>.domain.User;
<%_ if (authenticationType === 'session') { _%>
import <%=packageName%>.repository.PersistentTokenRepository;
<%_ } _%>
import <%=packageName%>.repository.UserRepository;
<%_ if (authenticationType !== 'oauth2') { _%>
import <%=packageName%>.security.SecurityUtils;
import <%=packageName%>.service.MailService;
<%_ } _%>
import <%=packageName%>.service.UserService;
import <%=packageName%>.service.dto.UserDTO;
<%_ if (authenticationType !== 'oauth2') { _%>
import <%=packageName%>.web.rest.errors.*;
import <%=packageName%>.web.rest.vm.KeyAndPasswordVM;
import <%=packageName%>.web.rest.vm.ManagedUserVM;

import org.apache.commons.lang3.StringUtils;
<%_ } _%>
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
<%_ if (authenticationType === 'oauth2') { _%>
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
<%_ } _%>

import javax.servlet.http.HttpServletRequest;
<%_ if (authenticationType !== 'oauth2') { _%>
import javax.validation.Valid;
<%_ } _%>
<%_ if (authenticationType === 'session') { _%>
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
<%_ } _%>
<%_ if (authenticationType === 'oauth2') { _%>
import java.security.Principal;
import java.time.Instant;
import java.util.stream.Collectors;
<%_ } _%>
import java.util.*;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    private final UserRepository userRepository;

    private final UserService userService;
    <%_ if (authenticationType !== 'oauth2') { _%>

    private final MailService mailService;
    <%_ } _%>
    <%_ if (authenticationType === 'session') { _%>

    private final PersistentTokenRepository persistentTokenRepository;
    <%_ } _%>

    public AccountResource(UserRepository userRepository, UserService userService<% if (authenticationType !== 'oauth2') { %>, MailService mailService<% } %><% if (authenticationType === 'session') { %>, PersistentTokenRepository persistentTokenRepository<% } %>) {

        this.userRepository = userRepository;
        this.userService = userService;
        <%_ if (authenticationType !== 'oauth2') { _%>
        this.mailService = mailService;
        <%_ } _%>
        <%_ if (authenticationType === 'session') { _%>
        this.persistentTokenRepository = persistentTokenRepository;
        <%_ } _%>
    }
<%_ if (authenticationType === 'oauth2') { _%>

    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/authenticate")
    @Timed
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET  /account : get the current user.
     *
     * @return the current user
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be returned
     */
    @GetMapping("/account")
    @Timed
    @SuppressWarnings("unchecked")
    public UserDTO getAccount(Principal principal) {
        if (principal != null) {
            if (principal instanceof OAuth2Authentication) {
                OAuth2Authentication authentication = (OAuth2Authentication) principal;
                LinkedHashMap<String, Object> details = (LinkedHashMap) authentication.getUserAuthentication().getDetails();

                User user = new User();
                user.setLogin(details.get("preferred_username").toString());

                if (details.get("given_name") != null) {
                    user.setFirstName((String) details.get("given_name"));
                }
                if (details.get("family_name") != null) {
                    user.setFirstName((String) details.get("family_name"));
                }
                if (details.get("email_verified") != null) {
                    user.setActivated((Boolean) details.get("email_verified"));
                }
                if (details.get("email") != null) {
                    user.setEmail((String) details.get("email"));
                }
                if (details.get("locale") != null) {
                    String locale = (String) details.get("locale");
                    String langKey = locale.substring(0, locale.indexOf("-"));
                    user.setLangKey(langKey);
                }

                Set<Authority> userAuthorities;

                // get roles from details
                if (details.get("roles") != null) {
                    List<String> roles = (List) details.get("roles");
                    userAuthorities = roles.stream()
                        .filter(role -> role.startsWith("ROLE_"))
                        .map(role -> {
                            Authority userAuthority = new Authority();
                            userAuthority.setName(role);
                            return userAuthority;
                        })
                        .collect(Collectors.toSet());
                    // if roles don't exist, try groups
                } else if (details.get("groups") != null) {
                    List<String> groups = (List) details.get("groups");
                    userAuthorities = groups.stream()
                        .filter(group -> group.startsWith("ROLE_"))
                        .map(group -> {
                            Authority userAuthority = new Authority();
                            userAuthority.setName(group);
                            return userAuthority;
                        })
                        .collect(Collectors.toSet());
                } else {
                    userAuthorities = authentication.getAuthorities().stream()
                        .map(role -> {
                            Authority userAuthority = new Authority();
                            userAuthority.setName(role.getAuthority());
                            return userAuthority;
                        })
                        .collect(Collectors.toSet());
                }

                user.setAuthorities(userAuthorities);
                UserDTO userDTO = new UserDTO(user);

                // convert Authorities to GrantedAuthorities
                Set<GrantedAuthority> grantedAuthorities = new LinkedHashSet<>();
                userAuthorities.forEach(authority -> {
                    grantedAuthorities.add(new SimpleGrantedAuthority(authority.getName()));
                });

                // create UserDetails so #{principal.username} works
                UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getLogin(),
                    "N/A", grantedAuthorities);
                // update Spring Security Authorities to match groups claim from IdP
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    userDetails, "N/A", grantedAuthorities);
                token.setDetails(details);
                authentication = new OAuth2Authentication(authentication.getOAuth2Request(), token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // save account in to sync users between IdP and JHipster's local database
                Optional<User> existingUser = userRepository.findOneByLogin(userDTO.getLogin());
                if (existingUser.isPresent()) {
                    // if IdP sends last updated information, use it to determine if an update should happen
                    if (details.get("updated_at") != null) {
                        Instant dbModifiedDate = existingUser.get().getLastModifiedDate();
                        Instant idpModifiedDate = new Date(Long.valueOf((Integer) details.get("updated_at"))).toInstant();
                        if (idpModifiedDate.isAfter(dbModifiedDate)) {
                            log.debug("Updating user '{}' in local database...", userDTO.getLogin());
                            userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(),
                                userDTO.getLangKey(), userDTO.getImageUrl());
                        }
                        // no last updated info, blindly update
                    } else {
                        log.debug("Updating user '{}' in local database...", userDTO.getLogin());
                        userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(),
                            userDTO.getLangKey(), userDTO.getImageUrl());
                    }
                } else {
                    log.debug("Saving user '{}' in local database...", userDTO.getLogin());
                    userRepository.save(user);
                }
                return userDTO;
            } else {
                // Allow Spring Security Test to be used to mock users in the database
                return Optional.ofNullable(userService.getUserWithAuthorities())
                    .map(UserDTO::new)
                    .orElseThrow(RuntimeException::new);
            }
        } else {
            throw new RuntimeException();
        }
    }
<%_ } else { _%>

    /**
     * POST  /register : register the user.
     *
     * @param managedUserVM the managed user View Model
     * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
     * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
     * @throws LoginAlreadyUsedException 400 (Bad Request) if the login is already used
     */
    @PostMapping("/register")
    @Timed
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        if (!checkPasswordLength(managedUserVM.getPassword())) {
            throw new InvalidPasswordException();
        }
        userRepository.findOneByLogin(managedUserVM.getLogin().toLowerCase()).ifPresent(u -> {throw new LoginAlreadyUsedException();});
        userRepository.findOneByEmailIgnoreCase(managedUserVM.getEmail()).ifPresent(u -> {throw new EmailAlreadyUsedException();});
        User user = userService.registerUser(managedUserVM);
        mailService.sendActivationEmail(user);
    }

    /**
     * GET  /activate : activate the registered user.
     *
     * @param key the activation key
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be activated
     */
    @GetMapping("/activate")
    @Timed
    public void activateAccount(@RequestParam(value = "key") String key) {
        userService.activateRegistration(key).orElseThrow(RuntimeException::new);
    }

    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/authenticate")
    @Timed
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET  /account : get the current user.
     *
     * @return the current user
     * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be returned
     */
    @GetMapping("/account")
    @Timed
    public UserDTO getAccount() {
        return Optional.ofNullable(userService.getUserWithAuthorities())
            .map(UserDTO::new)
            .orElseThrow(RuntimeException::new);
    }

    /**
     * POST  /account : update the current user information.
     *
     * @param userDTO the current user information
     * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
     * @throws RuntimeException 500 (Internal Server Error) if the user login wasn't found
     */
    @PostMapping("/account")
    @Timed
    public void saveAccount(@Valid @RequestBody UserDTO userDTO) {
        final String userLogin = SecurityUtils.getCurrentUserLogin();
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (existingUser.isPresent() && (!existingUser.get().getLogin().equalsIgnoreCase(userLogin))) {
            throw new EmailAlreadyUsedException();
        }
        userRepository.findOneByLogin(userLogin).orElseThrow(RuntimeException::new);
        userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(),
                    userDTO.getLangKey()<% if (databaseType === 'mongodb' || databaseType === 'sql') { %>, userDTO.getImageUrl()<% } %>);
    }

    /**
     * POST  /account/change-password : changes the current user's password
     *
     * @param password the new password
     * @throws InvalidPasswordException 400 (Bad Request) if the new password is incorrect
     */
    @PostMapping(path = "/account/change-password")
    @Timed
    public void changePassword(@RequestBody String password) {
        if (!checkPasswordLength(password)) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(password);
    }<% if (authenticationType === 'session') { %>

    /**
     * GET  /account/sessions : get the current open sessions.
     *
     * @return the current open sessions
     * @throws RuntimeException 500 (Internal Server Error) if the current open sessions couldn't be retrieved
     */
    @GetMapping("/account/sessions")
    @Timed
    public List<PersistentToken> getCurrentSessions() {
        return persistentTokenRepository.findByUser(
            userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin())
                .orElseThrow(RuntimeException::new)
        );
    }

    /**
     * DELETE  /account/sessions?series={series} : invalidate an existing session.
     *
     * - You can only delete your own sessions, not any other user's session
     * - If you delete one of your existing sessions, and that you are currently logged in on that session, you will
     *   still be able to use that session, until you quit your browser: it does not work in real time (there is
     *   no API for that), it only removes the "remember me" cookie
     * - This is also true if you invalidate your current session: you will still be able to use it until you close
     *   your browser or that the session times out. But automatic login (the "remember me" cookie) will not work
     *   anymore.
     *   There is an API to invalidate the current session, but there is no API to check which session uses which
     *   cookie.
     *
     * @param series the series of an existing session
     * @throws UnsupportedEncodingException if the series couldnt be URL decoded
     */
    @DeleteMapping("/account/sessions/{series}")
    @Timed
    public void invalidateSession(@PathVariable String series) throws UnsupportedEncodingException {
        String decodedSeries = URLDecoder.decode(series, "UTF-8");
        userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).ifPresent(u ->
            persistentTokenRepository.findByUser(u).stream()
                .filter(persistentToken -> StringUtils.equals(persistentToken.getSeries(), decodedSeries))<% if (databaseType === 'sql' || databaseType === 'mongodb') { %>
                .findAny().ifPresent(t -> persistentTokenRepository.delete(decodedSeries)));<% } else { %>
                .findAny().ifPresent(persistentTokenRepository::delete));<% } %>
    }<% } %>

    /**
     * POST   /account/reset-password/init : Send an email to reset the password of the user
     *
     * @param mail the mail of the user
     * @throws EmailNotFoundException 400 (Bad Request) if the email address is not registered
     */
    @PostMapping(path = "/account/reset-password/init")
    @Timed
    public void requestPasswordReset(@RequestBody String mail) {
        mailService.sendPasswordResetMail(
            userService.requestPasswordReset(mail)
                .orElseThrow(EmailNotFoundException::new)
        );
    }

    /**
     * POST   /account/reset-password/finish : Finish to reset the password of the user
     *
     * @param keyAndPassword the generated key and the new password
     * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
     * @throws RuntimeException 500 (Internal Server Error) if the password could not be reset
     */
    @PostMapping(path = "/account/reset-password/finish")
    @Timed
    public void finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())
            .orElseThrow(RuntimeException::new);
    }

    private static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
            password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH &&
            password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH;
    }
<%_ } _%>
}
<%_ } _%>
