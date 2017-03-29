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

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.User;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitUser;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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

    public SpeechletResponse handleUserSetup(Session session, Intent intent) {
        Validate.notNull(session);
        Map<String, Object> attributes = session.getAttributes();
        Validate.notNull(attributes);

        if (!attributes.containsKey(SETUP_ATTRIBUTE)) {
            log.info("Prompting user to setup home address.");
            session.setAttribute(SETUP_ATTRIBUTE, SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS);
            String homeAddressMessage = "In order to give you transit information, " +
                    "I first need your home address, with zip code. For example, you can say, my home address " +
                    "is, Fifteen Zero Nine Blakeley Street, Seattle, Washington, Nine Eight Three Three Zero.";
            String cardTitle = "Home Address";
            return getNewAskResponse(homeAddressMessage, cardTitle);
        }
        log.info("User setup has started");
        return handleAddressInputResponse(session, intent);
    }

    public SpeechletResponse handleUpdateHomeAddressRequest(Session session) {
        Validate.notNull(session);

        session.setAttribute(SETUP_ATTRIBUTE, SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS);

        return getNewAskResponse("Ok. If  you'd like to change your home address, tell me your" +
                " new home address, with zip code. For Example, you can say, my home address is" +
                " ,Nineteen Twenty Twenty Fourth, San Francisco, California, Nine Four Zero Four Four.",
                "Change Home Address");
    }

    public SpeechletResponse handleUpdateWorkAddressRequest(Session session) {
        Validate.notNull(session);

        session.setAttribute(SETUP_ATTRIBUTE, SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS);

        return getNewAskResponse("Ok. If  you'd like to change your work address, tell me your" +
                        " new work address, with zip code. For Example, you can say, my work address is" +
                        " ,Nineteen Twenty Sixteenth Avenue, San Francisco, California, Nine Four Zero Four Three.",
                "Change Work Address");
    }

    public SpeechletResponse handleUpdatePostalAddressRequest(Session session, Intent intent) {
        Validate.notNull(session);
        Validate.notNull(intent);

        String setupAttribute = (String) session.getAttribute(SETUP_ATTRIBUTE);
        log.info("Setup Attribute on update postal address request: " + setupAttribute);

        if (setupAttribute == null) {
            return getTryAgainResponse();
        }

        if (setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS)) {
            return verifyAddressResponse(intent, session, HOME_ADDRESS_ATTRIBUTE, "home");
        } else if (setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS)) {
            return verifyAddressResponse(intent, session, WORK_ADDRESS_ATTRIBUTE, WORK_KEY);
        }
        return getTryAgainResponse();
    }

    public SpeechletResponse handleVerifyPostalAddressRequest(Session session, Intent intent) {
        Validate.notNull(session);
        Validate.notNull(intent);

        String intentName = intent.getName();

        if (intentName.equals(YES_INTENT)) {
            String setupAttribute = (String) session.getAttribute(SETUP_ATTRIBUTE);
            User user = session.getUser();
            String userId = user.getUserId();
            log.info("Update address attribute: " + setupAttribute + ", User: " + userId);

            if (setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS)) {
                String homeAddressValue = (String) session.getAttribute(HOME_ADDRESS_ATTRIBUTE);

                if (homeAddressValue == null) {
                    return getTryAgainResponse();
                }
                return updateHomeAddressInDatabaseAndRespond(userId, homeAddressValue);
            } else if (setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS)) {
                String workAddressValue = (String) session.getAttribute(WORK_ADDRESS_ATTRIBUTE);

                if (workAddressValue == null) {
                    return getTryAgainResponse();
                }
                return updateWorkAddressInDatabaseAndRespond(userId, workAddressValue);
            }
        }
        return getNewAskResponse("Ok. Let's try again with the address", "Try again.");
    }

    public SpeechletResponse handleGetWorkAddressRequest(String userId) {
        return getAddress(userId, user -> {
            String destinationNotExistMessage = "Sorry, I cannot find your work address." +
                    " To add or update your work address, you can say, change my work address. ";
            String destinationNotExistTitle = "Work Address not found.";
            Map<String, String> destinations = user.getDestinations();

            if (destinations == null || !destinations.containsKey(WORK_KEY)) {
                return getNewTellResponse(destinationNotExistMessage,
                        destinationNotExistTitle);
            }
            String workAddress = destinations.get(WORK_KEY);

            if (workAddress == null || workAddress.isEmpty()) {
                return getNewTellResponse(destinationNotExistMessage,
                        destinationNotExistTitle);
            }
            return getNewTellResponse("Sure. Your work address is, " + workAddress, "Work Address");
        });
    }

    public SpeechletResponse handleGetHomeAddressRequest(String userId) {
        return getAddress(userId, user -> {
            String homeNotExistMessage = "Sorry, I cannot find your home address." +
                    " To add or update your home address, you can say, change my work address. ";
            String homeNotExistTitle = "Home Address not found.";
            String homeAddress = user.getHomeAddress();

            if (homeAddress == null || homeAddress.isEmpty()) {
                return getNewTellResponse(homeNotExistMessage, homeNotExistTitle);
            }
            return getNewTellResponse("Sure. Your home address is, "
                    + homeAddress, "Home Address");
        });
    }

    private SpeechletResponse getAddress(String userId,
                                         Function<TransitUser, SpeechletResponse> addressResponse) {
        Validate.notNull(userId);
        TransitUser user = userStore.getUser(userId);

        if (user == null) {
            return getNewTellResponse("Sorry, I cannot find your information. ",
                    "User not found");
        }
        return addressResponse.apply(user);
    }

    private SpeechletResponse updateHomeAddressInDatabaseAndRespond(String userId, String homeAddress) {
        try {
            TransitUser updatedUser = userStore.updateHomeAddress(userId, homeAddress);
            log.info("Updated user home address: " + updatedUser.getHomeAddress());
            return getNewTellResponse("OK. I changed your home address.", "Home address changed");
        } catch (Exception ex) {
            log.error("Could not update home address: ", ex);
            return getTryAgainResponse();
        }
    }

    private SpeechletResponse updateWorkAddressInDatabaseAndRespond(String userId, String workAddress) {
        try {
            userStore.addOrUpdateDestination(userId, WORK_KEY, workAddress);
            log.info("Updated user home address: " + workAddress);
            return getNewTellResponse("OK. I changed your work address.", "Home work changed");
        } catch (Exception ex) {
            log.error("Could not update work address: ", ex);
            return getTryAgainResponse();
        }
    }

    /**
     * Here, we're using a combination of intent name and session attributes to drive
     * the user setup conversation
     */
    private SpeechletResponse handleAddressInputResponse(Session session,
                                                         Intent intent) {
        String setupAttribute = (String) session.getAttribute(SETUP_ATTRIBUTE);
        String intentName = intent.getName();
        log.info("Intent Name: " + intentName
                + "\tSetup Attribute: " + setupAttribute);

        if (Arrays.asList(ADDRESS_INTENTS).contains(intentName) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS))
        {
            return verifyAddressResponse(intent, session,
                    HOME_ADDRESS_ATTRIBUTE, "home");
        }
        else if (intentName.equals(YES_INTENT) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS))
        {
            session.setAttribute(SETUP_ATTRIBUTE, SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS);
            return getNewAskResponse("Ok. Now tell me your work address, with zip code. For example, you can say" +
                    ", my work address is Twenty Four Hundred Martin Street, Seattle, Washington," +
                            " Nine Eight One One Four",
                    "Work Address");
        }
        else if (intentName.equals(NO_INTENT) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_HOME_ADDRESS))
        {
            return getNewAskResponse("Ok. Let's try again with your Home Address.", "Home Address");
        }
        else if (Arrays.asList(ADDRESS_INTENTS).contains(intentName) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS))
        {
            return verifyAddressResponse(intent, session,
                    WORK_ADDRESS_ATTRIBUTE, WORK_KEY);
        }
        else if (intentName.equals(NO_INTENT) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS))
        {
            return getNewAskResponse("Ok. Let's try again with your Work Address.", "Work Address");
        }
        else if (intentName.equals(YES_INTENT) &&
                setupAttribute.equals(SETUP_ATTRIBUTE_VALUE_WORK_ADDRESS))
        {
            // Finally the setup process has completed. Let's add the details in the database
            return addUserToDatabaseAndReturnSuccess(session);
        }
        else
        {
            return getTryAgainResponse();
        }
    }

    private SpeechletResponse addUserToDatabaseAndReturnSuccess(Session session) {
        String userId = session.getUser().getUserId();
        String homeAddress = (String) session.getAttribute(HOME_ADDRESS_ATTRIBUTE);
        String workAddress = (String) session.getAttribute(WORK_ADDRESS_ATTRIBUTE);

        Map<String, String> destinations = new HashMap<>();
        destinations.put(WORK_KEY, workAddress);
        TransitUser user = null;

        try {
            log.info("Attempting to insert user: " + userId);
            user = userStore.upsertUser(userId, homeAddress, destinations);
            log.info("Inserted user: " + user);
        } catch (Exception ex) {
            log.error("Could not insert user into the TransitUsers table.", ex);
            return getNewAskResponse("Sorry. I'm having some issues entering your details. Please try again. ",
                    "Try again. ");

        }
        log.info("User setup successful. User: " + user);
        return getNewAskResponse("OK. I have everything I need. Now I can help you with " +
                        "transit information. For example, you can ask me, \'When\'s my next bus to work.\'",
                "User Setup completed.");
    }

    /**
     * Verify from the user whether the address is correctly understood
     */
    private SpeechletResponse verifyAddressResponse(Intent intent,
                                                    Session session,
                                                    String attribute,
                                                    String addressName) {
        Slot slot = intent.getSlot(ADDRESS_SLOT);
        String addressValue = slot.getValue();
        log.info("Setting address: " + addressValue);
        String resolvedAddress = googleMaps.getAddressOfPlace(addressValue);

        if (resolvedAddress == null || resolvedAddress.isEmpty()) {
            return getNewAskResponse("Sorry. I could not find this address. Please try again. ",
                    "Try Again.");
        }
        log.info("Understood address from user to be: " + resolvedAddress);
        session.setAttribute(attribute, resolvedAddress);
        return getNewAskResponse("Ok. I understood your " +
                        addressName + " address to be, " + resolvedAddress + ". Is this correct?",
                        addressName + " address");
    }

    private SpeechletResponse getTryAgainResponse() {
        return getNewAskResponse("Sorry. I did not understand. Please try again. ",
                "Try Again.");
    }

    private SpeechletResponse getNewAskResponse(String output, String title) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(output);
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(output);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt, card);
    }

    private SpeechletResponse getNewTellResponse(String output, String title) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(output);
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(output);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);
        return SpeechletResponse.newTellResponse(outputSpeech, card);
    }
}
