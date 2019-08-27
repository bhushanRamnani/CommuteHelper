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


import com.amazon.ask.model.services.ServiceException;
import com.amazon.ask.model.services.deviceAddress.Address;
import com.amazon.ask.model.services.deviceAddress.DeviceAddressServiceClient;

import com.amazon.ask.model.ui.AskForPermissionsConsentCard;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitUser;
import com.ramnani.alexaskills.CommuteHelper.util.AlexaUtils;
import com.ramnani.alexaskills.CommuteHelper.util.Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;

import java.util.Arrays;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * This class owns the functionality of setting up user details
 * including home address and work address
 */
public class UserSetupSpeechletManager extends CommuteHelperSpeechletManager {

    public static final String DESTINATION_ATTRIBUTE = "destination";
    public static final String DESTINATION_VALUE_SETUP_QUESTION_PHASE = "setupQuestionPhase";
    public static final String DESTINATION_VALUE_SETUP_LOCATION_NAME_PHASE = "locationNamePhase";
    public static final String DESTINATION_VALUE_SETUP_LOCATION_ADDRESS_PHASE = "locationAddressPhase";
    public static final String DESTINATION_NAME_ATTRIBUTE = "destinationName";
    public static final String DESTINATION_ADDRESS_ATTRIBUTE = "destinationAddress";

    private static final String YES_INTENT = "YesIntent";
    private static final String NO_INTENT = "NoIntent";

    private static final String LOCATION_NAME_SLOT = "location";
    private static final String LOCATION_ADDRESS_SLOT = "address";
    private static final String WORK_KEY = "work";
    private static final String LOCATION_NAME_EXAMPLE_SPEECH =
            "For Example, you can say, the location name is work or the location name is grocery store.";

    private static final String LOCATION_ADDRESS_EXAMPLE_SPEECH = "For example, you can say,"
            + " the location address is Nineteen Twenty Sixteenth Avenue,"
            + " San Francisco, California, Nine Four Zero Four Three";

    private static final Logger log = Logger.getLogger(UserSetupSpeechletManager.class);

    public UserSetupSpeechletManager(TransitHelperDao userStore, GoogleMapsService googleMaps) {
        super(userStore, googleMaps);
    }

    public Optional<Response> handleUserSetup(HandlerInput handlerInput) {
        Validator.validateHandlerInput(handlerInput);

        log.info("Handling user setup: " + handlerInput.getRequestEnvelope());

        ImmutablePair<Optional<String>, Optional<Response>> updateHomeAddressResponse
                = super.updateHomeAddressFromDeviceAddress(handlerInput);

        Validate.isTrue(updateHomeAddressResponse.getLeft().isPresent()
                || updateHomeAddressResponse.getRight().isPresent());

        if (updateHomeAddressResponse.getRight().isPresent()) {
            return updateHomeAddressResponse.getRight();
        }

        String successSpeech = "You could either add a specific destination by address or directly ask me for"
                + " transit information if it’s a well-known location."
                + " Would you like to add a destination by address?";

        handlerInput.getAttributesManager()
                .getSessionAttributes().put(DESTINATION_ATTRIBUTE, DESTINATION_VALUE_SETUP_QUESTION_PHASE);

        return handlerInput.getResponseBuilder()
                .withSpeech(successSpeech)
                .withSimpleCard("Your home address", successSpeech)
                .withShouldEndSession(true)
                .build();
    }

    public Optional<Response> handlePutLocationAddress(HandlerInput handlerInput, Intent intent) {
        Validator.validateHandlerInput(handlerInput);
        Validator.validateIntent(intent);

        Map<String, Object> session = handlerInput.getAttributesManager().getSessionAttributes();

        if (session.get(DESTINATION_NAME_ATTRIBUTE) == null
                || StringUtils.isBlank((String) session.get(DESTINATION_NAME_ATTRIBUTE))) {
            log.warn("Could not find value for the location name atteribute. Request: "
                    + handlerInput.getRequestEnvelope());
            return getNewAskResponse("Ok. Please tell me the location name first. "
                + LOCATION_NAME_EXAMPLE_SPEECH, "Location name", handlerInput);
        }
        String locationName = (String) session.get(DESTINATION_NAME_ATTRIBUTE);

        Map<String, Slot> slots = intent.getSlots();

        Optional<Response> tryAgainResponse = getNewAskResponse(
                "Sorry. I could not understand the location address. Please try again. "
                        + LOCATION_ADDRESS_EXAMPLE_SPEECH, "Location Address", handlerInput);

        if (slots == null || slots.get(LOCATION_ADDRESS_SLOT) == null) {
            log.warn("Could not get slot " + LOCATION_ADDRESS_SLOT + " for request: "
                    + handlerInput.getRequestEnvelope());
            return tryAgainResponse;
        }
        final String addressValue = slots.get(LOCATION_ADDRESS_SLOT).getValue();
        final String googleResolvedAddress = googleMaps.getAddressOfPlace(addressValue);

        if (StringUtils.isBlank(googleResolvedAddress)) {
            log.warn("Could not resolve location address from google. Slot address value: "
                    + addressValue);

            return tryAgainResponse;
        }
        session.put(DESTINATION_ADDRESS_ATTRIBUTE, googleResolvedAddress);

        return getNewAskResponse("Ok. I understood the address for " + locationName
                + " to be " + googleResolvedAddress + ". Is this correct? ",
                "Location Address", handlerInput);
    }

    public Optional<Response> handlePutLocationName(HandlerInput handlerInput, Intent intent) {
        Validator.validateHandlerInput(handlerInput);
        Validator.validateIntent(intent);

        Map<String, Slot> slots = intent.getSlots();

        Optional<Response> tryAgainResponse = getNewAskResponse(
                "Sorry, I could not understand the location name. Please"
                + " tell me again. " + LOCATION_NAME_EXAMPLE_SPEECH, "Location Name", handlerInput);

        if (slots == null || slots.get(LOCATION_NAME_SLOT) == null) {
            log.warn("slots was null or slot did not have location name: "
                    + handlerInput.getRequestEnvelope());
            return tryAgainResponse;
        }
        Slot locationSlot = slots.get(LOCATION_NAME_SLOT);
        String locationSlotValue = locationSlot.getValue();

        if (StringUtils.isBlank(locationSlotValue)) {
            log.warn("Value of location slot " + LOCATION_NAME_SLOT + " was blank.");
            return tryAgainResponse;
        }
        log.info("Updating " + DESTINATION_NAME_ATTRIBUTE + " to " + locationSlotValue);
        Map<String, Object> session = handlerInput.getAttributesManager().getSessionAttributes();
        session.put(DESTINATION_NAME_ATTRIBUTE, locationSlotValue);
        return getNewAskResponse("Ok. I understood the location name to be "
                 + locationSlotValue + ". Is this correct ?", "Location Name", handlerInput);
    }

    public Optional<Response> handleDestinationSetup(HandlerInput handlerInput, Intent intent) {
        Validator.validateHandlerInput(handlerInput);
        Validator.validateIntent(intent);
        Validate.isTrue(intent.getName().equals(YES_INTENT) || intent.getName().equals(NO_INTENT));

        log.info("Handling Destination setup: " + handlerInput.getRequestEnvelope());

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager().getSessionAttributes();
        String destinationAttributeValue = (String) sessionAttributes.get(DESTINATION_ATTRIBUTE);

        Validate.notBlank(destinationAttributeValue);
        Validate.isTrue(
                Arrays.asList(
                        DESTINATION_VALUE_SETUP_QUESTION_PHASE,
                        DESTINATION_VALUE_SETUP_LOCATION_NAME_PHASE,
                        DESTINATION_VALUE_SETUP_LOCATION_ADDRESS_PHASE
                ).contains(destinationAttributeValue)
        );

        if (intent.getName().equals(YES_INTENT)) {
            if (destinationAttributeValue.equals(DESTINATION_VALUE_SETUP_QUESTION_PHASE)) {
                // Tell customer to enter destination name
                sessionAttributes.put(DESTINATION_ATTRIBUTE, DESTINATION_VALUE_SETUP_LOCATION_NAME_PHASE);

                return getNewAskResponse("OK. Please give me a name for the location. "
                                + LOCATION_NAME_EXAMPLE_SPEECH,
                        "Location Name",
                        handlerInput);
            } else if (destinationAttributeValue.equals(DESTINATION_VALUE_SETUP_LOCATION_NAME_PHASE)) {
                sessionAttributes.put(DESTINATION_ATTRIBUTE, DESTINATION_VALUE_SETUP_LOCATION_ADDRESS_PHASE);

                return getNewAskResponse("OK. Please tell me the location address with zip code. "
                        + LOCATION_ADDRESS_EXAMPLE_SPEECH,
                        "Location Address",
                        handlerInput);
            } else {
                updateDestinationAddress(handlerInput);
            }
        } else {
            if (destinationAttributeValue.equals(DESTINATION_VALUE_SETUP_QUESTION_PHASE)) {
                // Ask customer if they want transit information for a well known location
                // TODO: Figure out a better example for a well-known location
                sessionAttributes.remove(DESTINATION_ATTRIBUTE);
                return getNewAskResponse("OK. You can then ask me for transit information to a well-known" +
                                " place close to where you live. For example, you can ask me when's the next bus" +
                                " to the nearest train station.",
                        "Get Transit Information",
                        handlerInput);
            } else if (destinationAttributeValue.equals(DESTINATION_VALUE_SETUP_LOCATION_NAME_PHASE)) {
                // Ask customer to try saying the location name again
                return getNewAskResponse("OK. Please try again. " + LOCATION_NAME_EXAMPLE_SPEECH,
                        "Location Name",
                        handlerInput);
            } else {
                // Ask customer to try saying the address again
                return getNewAskResponse("OK. Please try again. " + LOCATION_ADDRESS_EXAMPLE_SPEECH,
                        "Location Address",
                        handlerInput);
            }
        }
        throw new IllegalArgumentException("Unexpected session state for this function: "
                + handlerInput.getRequestEnvelope());
    }

    private void updateDestinationAddress(HandlerInput handlerInput) {
        Map<String, Object> sessionAttributes = handlerInput
                .getAttributesManager().getSessionAttributes();

        String locationName = (String) sessionAttributes.get(DESTINATION_NAME_ATTRIBUTE);
        String locationAddress = (String) sessionAttributes.get(DESTINATION_ADDRESS_ATTRIBUTE);

        Validate.notBlank(locationName);
        Validate.notBlank(locationAddress);

        String userId = handlerInput.getRequestEnvelope().getSession().getUser().getUserId();

        log.info("Adding destination " + locationName + " for userId: " + userId);
        userStore.addOrUpdateDestination(userId, locationName, locationAddress);
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
                    " To add or update your home address, you can say, change my home address. ";
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
