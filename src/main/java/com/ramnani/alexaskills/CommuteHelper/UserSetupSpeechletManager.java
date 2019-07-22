/*
 * Copyright 2016-2017 Bhushan Ramnani (b.ramnani@gmail.com),
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ramnani.alexaskills.CommuteHelper;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.model.User;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitUser;
import com.ramnani.alexaskills.CommuteHelper.util.Validator;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * This class owns the functionality of setting up user details
 * including home address and work address
 */
public class UserSetupSpeechletManager {

    public static final String SETUP_ATTRIBUTE = "setupAttribute";
    private static final String HOME_ADDRESS_ATTRIBUTE = "homeAddress";
    private static final String WORK_ADDRESS_ATTRIBUTE = "workAddress";

    private static final String SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS = "homeAddressSetup";
    private static final String SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS = "workAddressSetup";

    private static final String[] ADDRESS_INTENTS = {"PutPostalAddress", "PutStreetAddress"};
    private static final String YES_INTENT = "YesIntent";
    private static final String NO_INTENT = "NoIntent";
    private static final String ADDRESS_SLOT = "address";
    private static final String WORK_KEY = "work";

    private static final Logger log = Logger.getLogger(UserSetupSpeechletManager.class);

    private TransitHelperDao userStore;
    private GoogleMapsService googleMaps;

    public UserSetupSpeechletManager(TransitHelperDao userStore, GoogleMapsService googleMaps) {
        Validate.notNull(userStore);
        Validate.notNull(googleMaps);

        this.userStore = userStore;
        this.googleMaps = googleMaps;
    }

    public Optional<Response> handleUserSetup(HandlerInput handlerInput, Intent intent) {
        Validator.validateHandlerInput(handlerInput);
        Validator.validateIntent(intent);

        log.info("Handling user setup: " + handlerInput.getRequestEnvelope());

        Map<String, Object> attributes = handlerInput.getAttributesManager().getSessionAttributes();

        if (!attributes.containsKey(SETUP_ATTRIBUTE)) {
            log.info("Prompting user to setup home address: " + handlerInput.getRequestEnvelope());
            attributes.put(SETUP_ATTRIBUTE, SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS);
            String homeAddressMessage = "In order to give you transit information, " +
                    "I first need your home address, with zip code. For example, you can say, my home address " +
                    "is, Fifteen Zero Nine Blakeley Street, Seattle, Washington, Nine Eight Three Three Zero.";
            String cardTitle = "Home Address";

            return handlerInput.getResponseBuilder()
                    .withSimpleCard(cardTitle, homeAddressMessage)
                    .withShouldEndSession(false)
                    .withReprompt(homeAddressMessage)
                    .withSpeech(homeAddressMessage)
                    .build();
        }
        log.info("User setup has started: " + handlerInput.getRequestEnvelope());
        return handleAddressInputResponse(handlerInput, intent);
    }

    public Optional<Response> handleUpdateHomeAddressRequest(HandlerInput handlerInput) {
        Validator.validateHandlerInput(handlerInput);
        log.info("Handling UpdateHomeAddress request: " + handlerInput.getRequestEnvelope());

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager().getSessionAttributes();

        sessionAttributes.put(SETUP_ATTRIBUTE, SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS);

        String output = "Ok. If  you'd like to change your home address, tell me your"
                + " new home address, with zip code. For Example, you can say, my home address is"
                + " ,Nineteen Twenty Twenty Fourth, San Francisco, California, Nine Four Zero Four Four.";

        return handlerInput.getResponseBuilder()
                .withSpeech(output)
                .withSimpleCard("Change Home Address", output)
                .withReprompt(output)
                .withShouldEndSession(false)
                .build();
    }

    public Optional<Response> handleUpdateWorkAddressRequest(HandlerInput handlerInput) {
        Validator.validateHandlerInput(handlerInput);
        log.info("Handling UpdateWorkAddress request: " + handlerInput.getRequestEnvelope());

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager().getSessionAttributes();

        sessionAttributes.put(SETUP_ATTRIBUTE, SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS);

        String output = "Ok. If  you'd like to change your work address, tell me your" +
                        " new work address, with zip code. For Example, you can say, my work address is" +
                        " ,Nineteen Twenty Sixteenth Avenue, San Francisco, California, Nine Four Zero Four Three.";

        return handlerInput.getResponseBuilder()
                .withSpeech(output)
                .withSimpleCard("Change Work Address", output)
                .withReprompt(output)
                .withShouldEndSession(false)
                .build();
    }

    public Optional<Response> handleUpdatePostalAddressRequest(HandlerInput handlerInput, Intent intent) {
        Validate.notNull(handlerInput);
        Validate.notNull(intent);

        log.info("Handling UpdatePostalAddress request: " + handlerInput.getRequestEnvelope());

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager().getSessionAttributes();

        String setupAttribute = (String) sessionAttributes.get(SETUP_ATTRIBUTE);
        log.info("Setup Attribute on update postal address request: " + setupAttribute);

        if (setupAttribute == null) {
            log.info(SETUP_ATTRIBUTE + " not found in session: " + handlerInput.getRequestEnvelope().getSession());
            return getTryAgainResponse(handlerInput);
        }

        if (setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS)) {
            log.info(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS + " found in session. Verifying home address response");
            return verifyAddressResponse(intent, handlerInput, HOME_ADDRESS_ATTRIBUTE, "home");
        } else if (setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS)) {
            log.info(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS + " found in session. Verifying work address response.");
            return verifyAddressResponse(intent, handlerInput, WORK_ADDRESS_ATTRIBUTE, WORK_KEY);
        }
        return getTryAgainResponse(handlerInput);
    }

    public Optional<Response> handleVerifyPostalAddressRequest(HandlerInput handlerInput, Intent intent) {
        Validator.validateHandlerInput(handlerInput);
        Validator.validateIntent(intent);

        log.info("Inside handleVerifyPostalAddressRequest. Request" + handlerInput.getRequestEnvelope());
        String intentName = intent.getName();
        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager().getSessionAttributes();

        if (intentName.equals(YES_INTENT)) {
            String setupAttribute = (String) sessionAttributes.get(SETUP_ATTRIBUTE);
            User user = handlerInput.getRequestEnvelope().getSession().getUser();
            String userId = user.getUserId();
            log.info(YES_INTENT + " received with setup attribute: " + SETUP_ATTRIBUTE + ". Value: " + setupAttribute);

            if (setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS)) {
                String homeAddressValue = (String) sessionAttributes.get(HOME_ADDRESS_ATTRIBUTE);

                if (homeAddressValue == null) {
                    log.info(HOME_ADDRESS_ATTRIBUTE + " is null. Asking user to try again.");
                    return getTryAgainResponse(handlerInput);
                }
                return updateHomeAddressInDatabaseAndRespond(userId, homeAddressValue, handlerInput);
            } else if (setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS)) {
                String workAddressValue = (String) sessionAttributes.get(WORK_ADDRESS_ATTRIBUTE);

                if (workAddressValue == null) {
                    log.info(WORK_ADDRESS_ATTRIBUTE + " is null. Asking user to try again.");
                    return getTryAgainResponse(handlerInput);
                }
                return updateWorkAddressInDatabaseAndRespond(userId, workAddressValue, handlerInput);
            }
        }
        return getNewAskResponse("Ok. Let's try again with the address", "Try again.", handlerInput);
    }

    public Optional<Response> handleGetWorkAddressRequest(HandlerInput handlerInput) {
        Validator.validateHandlerInput(handlerInput);

        String userId = handlerInput.getRequestEnvelope().getSession().getUser().getUserId();

        log.info("Inside handleGetWorkAddressRequest. Request: " + handlerInput.getRequestEnvelope());

        return getAddress(userId, user -> {
            String destinationNotExistMessage = "Sorry, I cannot find your work address." +
                    " To add or update your work address, you can say, change my work address. ";
            String destinationNotExistTitle = "Work Address not found.";
            Map<String, String> destinations = user.getDestinations();

            if (destinations == null || !destinations.containsKey(WORK_KEY)) {
                return getNewTellResponse(destinationNotExistMessage,
                        destinationNotExistTitle, handlerInput);
            }
            String workAddress = destinations.get(WORK_KEY);

            if (workAddress == null || workAddress.isEmpty()) {
                return getNewTellResponse(destinationNotExistMessage,
                        destinationNotExistTitle, handlerInput);
            }
            return getNewTellResponse("Sure. Your work address is, " + workAddress,
                    "Work Address", handlerInput);
        }, handlerInput);
    }

    public Optional<Response> handleGetHomeAddressRequest(HandlerInput handlerInput) {
        Validator.validateHandlerInput(handlerInput);
        log.info("Inside handleGetHomeAddressRequest. Request: " + handlerInput.getRequestEnvelope());

        String userId = handlerInput.getRequestEnvelope().getSession().getUser().getUserId();

        return getAddress(userId, user -> {
            String homeNotExistMessage = "Sorry, I cannot find your home address." +
                    " To add or update your home address, you can say, change my work address. ";
            String homeNotExistTitle = "Home Address not found.";
            String homeAddress = user.getHomeAddress();

            if (homeAddress == null || homeAddress.isEmpty()) {
                return getNewTellResponse(homeNotExistMessage, homeNotExistTitle, handlerInput);
            }
            return getNewTellResponse("Sure. Your home address is, "
                    + homeAddress, "Home Address", handlerInput);
        }, handlerInput);
    }

    public Optional<TransitUser> getTransitUser(HandlerInput handlerInput) {
        Validator.validateHandlerInput(handlerInput);

        String userId = handlerInput.getRequestEnvelope().getSession().getUser().getUserId();

        TransitUser transitUser = userStore.getUser(userId);
        tryUpdateTimezone(transitUser);
        return Optional.ofNullable(transitUser);
    }

    /**
     * This is to update the existing user's timezone
     * @param user
     */
    private void tryUpdateTimezone(TransitUser user) {
        if (user == null) {
            return;
        }
        String timezone = user.getTimeZone();

        if (timezone == null || timezone.length() == 0) {
            log.info("Trying to update timezone for user: " + user.getUserId());

            try {
                String homeAddress = user.getHomeAddress();
                timezone = googleMaps.getTimezoneFromAddress(homeAddress);
                userStore.addOrUpdateTimezone(user.getUserId(), timezone);
                log.info("Updated timezone: " + timezone +
                        ". For user: " + user.getUserId());
            } catch (Exception ex) {
                log.error("Unable to update timezone.", ex);
            }
        }
    }

    private Optional<Response> getAddress(String userId,
                                          Function<TransitUser, Optional<Response>> addressResponse,
                                          HandlerInput handlerInput) {
        Validate.notNull(userId);
        TransitUser user = userStore.getUser(userId);

        if (user == null) {
            return getNewTellResponse("Sorry, I cannot find your information. ",
                    "User not found", handlerInput);
        }
        return addressResponse.apply(user);
    }

    private Optional<Response> updateHomeAddressInDatabaseAndRespond(String userId,
                                                                     String homeAddress,
                                                                     HandlerInput handlerInput) {
        try {
            log.info("Updating home address for user: " + userId);
            userStore.updateHomeAddress(userId, homeAddress);
            log.info("Updated user home address: " + userId);

            log.info("Updating timezone for user: " + userId);

            try {
                String timezone = googleMaps.getTimezoneFromAddress(homeAddress);
                log.info("User timezone: " + timezone + ". UserId " +  userId);

                userStore.addOrUpdateTimezone(userId, timezone);
            } catch (Exception e1) {
                log.error("Could not update timezone.", e1);
            }
            return getNewTellResponse("OK. I changed your home address.",
                    "Home address changed", handlerInput);
        } catch (Exception ex) {
            log.error("Could not update home address: ", ex);
            return getTryAgainResponse(handlerInput);
        }
    }

    private Optional<Response> updateWorkAddressInDatabaseAndRespond(String userId,
                                                                     String workAddress,
                                                                     HandlerInput handlerInput) {
        try {
            userStore.addOrUpdateDestination(userId, WORK_KEY, workAddress);
            log.info("Updated user home address for userId: " + userId);
            return getNewTellResponse("OK. I changed your work address.",
                    "Home work changed", handlerInput);
        } catch (Exception ex) {
            log.error("Could not update work address: ", ex);
            return getTryAgainResponse(handlerInput);
        }
    }

    /**
     * Here, we're using a combination of intent name and session attributes to drive
     * the user setup conversation
     */
    private Optional<Response> handleAddressInputResponse(HandlerInput handlerInput,
                                                          Intent intent) {

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager().getSessionAttributes();
        String setupAttribute = (String) sessionAttributes.get(SETUP_ATTRIBUTE);
        String intentName = intent.getName();
        log.info("Intent Name: " + intentName
                + "\tSetup Attribute: " + setupAttribute);

        if (Arrays.asList(ADDRESS_INTENTS).contains(intentName) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS))
        {
            return verifyAddressResponse(intent, handlerInput,
                    HOME_ADDRESS_ATTRIBUTE, "home");
        }
        else if (intentName.equals(YES_INTENT) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS))
        {
            sessionAttributes.put(SETUP_ATTRIBUTE, SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS);
            return getNewAskResponse("Ok. Now tell me your work address, with zip code. For example, you can say" +
                    ", my work address is Twenty Four Hundred Martin Street, Seattle, Washington," +
                            " Nine Eight One One Four",
                    "Work Address", handlerInput);
        }
        else if (intentName.equals(NO_INTENT) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS))
        {
            return getNewAskResponse("Ok. Let's try again with your Home Address.",
                    "Home Address", handlerInput);
        }
        else if (Arrays.asList(ADDRESS_INTENTS).contains(intentName) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS))
        {
            return verifyAddressResponse(intent, handlerInput,
                    WORK_ADDRESS_ATTRIBUTE, WORK_KEY);
        }
        else if (intentName.equals(NO_INTENT) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS))
        {
            return getNewAskResponse("Ok. Let's try again with your Work Address.",
                    "Work Address", handlerInput);
        }
        else if (intentName.equals(YES_INTENT) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS))
        {
            // Finally the setup process has completed. Let's add the details in the database
            return addUserToDatabaseAndReturnSuccess(handlerInput);
        }
        else
        {
            return getTryAgainResponse(handlerInput);
        }
    }

    private Optional<Response> addUserToDatabaseAndReturnSuccess(HandlerInput handlerInput) {
        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager().getSessionAttributes();

        String userId = handlerInput.getRequestEnvelope().getSession().getUser().getUserId();
        String homeAddress = (String) sessionAttributes.get(HOME_ADDRESS_ATTRIBUTE);
        String workAddress = (String) sessionAttributes.get(WORK_ADDRESS_ATTRIBUTE);

        Map<String, String> destinations = new HashMap<>();
        destinations.put(WORK_KEY, workAddress);
        TransitUser user = null;
        String timeZone = null;

        try {
            timeZone = googleMaps.getTimezoneFromAddress(homeAddress);
        } catch (Exception ex) {
            log.error("Unable to obtain time zone from google maps API.", ex);
        }

        try {
            log.info("Attempting to insert user: " + userId);
            user = userStore.upsertUser(userId, homeAddress, destinations, timeZone);
            log.info("Inserted user: " + user);
        } catch (Exception ex) {
            log.error("Could not insert user into the TransitUsers table.", ex);
            return getNewAskResponse("Sorry. I'm having some issues entering your details. Please try again. ",
                    "Try again. ", handlerInput);

        }
        log.info("User setup successful. User: " + user);
        return getNewAskResponse("OK. I have everything I need. Now I can help you with " +
                        "transit information. For example, you can ask me, \'When\'s my next bus to work.\'",
                "User Setup completed.", handlerInput);
    }

    /**
     * Verify from the user whether the address is correctly understood
     */
    private Optional<Response> verifyAddressResponse(Intent intent,
                                                     HandlerInput handlerInput,
                                                     String attribute,
                                                     String addressName) {
        Map<String, Slot> slots = intent.getSlots();
        Validate.notNull(slots);

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager().getSessionAttributes();

        Slot slot = slots.get(ADDRESS_SLOT);
        String addressValue = slot.getValue();
        log.info("Setting address: " + addressValue);

        String resolvedAddress = googleMaps.getAddressOfPlace(addressValue);
        String output = "Sorry. I could not find this address. Please try again. ";

        if (resolvedAddress == null || resolvedAddress.isEmpty()) {
            return handlerInput.getResponseBuilder()
                    .withReprompt(output)
                    .withSpeech(output)
                    .withReprompt(output)
                    .withShouldEndSession(false)
                    .withSimpleCard("Try Again.", output)
                    .build();
        }
        log.info("Understood address from user to be: " + resolvedAddress);
        sessionAttributes.put(attribute, resolvedAddress);

        output = "Ok. I understood your " + addressName
                + " address to be, " + resolvedAddress + ". Is this correct?";

        return handlerInput.getResponseBuilder()
                .withReprompt(output)
                .withShouldEndSession(false)
                .withSpeech(output)
                .withSimpleCard(addressName + " address", output)
                .build();
    }

    private Optional<Response> getNewAskResponse(String output, String title, HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(output)
                .withSimpleCard(title, output)
                .withReprompt(output)
                .withShouldEndSession(false)
                .build();
    }

    private Optional<Response> getTryAgainResponse(HandlerInput handlerInput) {
        String output = "Sorry. I did not understand. Please try again. ";
        return handlerInput.getResponseBuilder()
                .withSpeech(output)
                .withReprompt(output)
                .withShouldEndSession(false)
                .withSimpleCard("Try Again.", output)
                .build();
    }

    private Optional<Response> getNewTellResponse(String output, String title, HandlerInput handlerInput) {
        return handlerInput.getResponseBuilder()
                .withSpeech(output)
                .withSimpleCard(title, output)
                .withShouldEndSession(true)
                .build();
    }
}
