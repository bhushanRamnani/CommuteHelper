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
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.google.maps.model.Duration;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitUser;
import com.ramnani.alexaskills.CommuteHelper.utils.SpeechletUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * This speechlet manager is responsible for speech responses to user
 * questions related to transit suggestions.
 */
public class TransitSpeechletManager {
    private static final Logger log = Logger.getLogger(TransitSpeechletManager.class);

    private static final String WORK_KEY = "work";

    private static final String SLOT_TRANSIT = "transit";

    public static final String SUGGESTION_ATTRIBUTE = "suggestion";

    private static final String REPROMPT_INTENT_ATTRIBUTE = "repromptIntent";

    private static final String INDEX_ATTRIBUTE = "index";

    private static final String PREVIOUS_RESPONSE_ATTRIBUTE = "previousResponse";

    private static final String DEFAULT_TIMEZONE = "America/Los_Angeles";

    private static final String HELP_STRING = "Sorry. I don't know that. " +
            "First, ask me for a transit suggestion. " +
            "For example, ask me when's my next bus";

    private static final String ERROR_STRING = "Sorry. I'm having some issues " +
            "giving you an answer right now.";

    private static Map<String, String> REPROMPT_QUESTIONS;

    private static final String TIME_FORMAT = "hh:mm a";

    private ObjectMapper mapper = new ObjectMapper();

    private GoogleMapsService googleMapsService;

    public TransitSpeechletManager(GoogleMapsService googleMapsService) {
        Validate.notNull(googleMapsService);
        this.googleMapsService = googleMapsService;

        REPROMPT_QUESTIONS = new HashMap<>();
        REPROMPT_QUESTIONS.put("GetArrivalTime",
                               " Would you like to know the arrival time?");
        REPROMPT_QUESTIONS.put("GetTotalTransitDuration",
                               " Would you like to know how long it will take to reach your destination?");
        REPROMPT_QUESTIONS.put("GetDirections",
                               " Would you like to get directions to your transit stop?");
        REPROMPT_QUESTIONS.put("AMAZON.NextIntent",
                               " Would you like to hear the next option?");
        REPROMPT_QUESTIONS.put("AMAZON.PreviousIntent",
                               " Would you like to hear the previous option?");
        REPROMPT_QUESTIONS.put("AMAZON.RepeatIntent",
                               " Would you like me to repeat this option?");
    }

    public SpeechletResponse handleNextTransitRequest(Intent intent,
                                                      Session session,
                                                      TransitUser user) throws IOException {
        Slot slot = intent.getSlot(SLOT_TRANSIT);
        String transitType = slot.getValue();
        String homeAddress = user.getHomeAddress();

        if (homeAddress == null || homeAddress.isEmpty()) {
            log.error("Sorry. Home Address does not exist for user: " + user.getUserId());
            return getErrorResponse("Home Address does not exist.");
        }
        Map<String, String> destinations = user.getDestinations();
        log.info("Destinations : " + destinations.toString() + ". User: " + user.getUserId());

        if (destinations == null || !destinations.containsKey(WORK_KEY)) {
            log.error("Sorry. Work address does not exist for user: " + user.getUserId());
            return getErrorResponse("Work address does not exist");
        }
        String workAddress = destinations.get(WORK_KEY);

        List<TransitSuggestion> suggestions = googleMapsService
                        .getNextTransitToDestination(transitType, homeAddress, workAddress);

        if (suggestions == null || suggestions.size() == 0) {
            log.warn("No Suggestions for user: " + user.getUserId());
            String speechText =
                    "Sorry. There are no available transit options " +
                            "for your destination at this time.";
            return getErrorResponse(speechText);
        }
        TransitSuggestion suggestion = suggestions.get(0);
        SpeechletResponse response = suggestionToDetailedResponse(suggestion, session,
                "Your next " + transitType + " is ", intent);
        session.setAttribute(SUGGESTION_ATTRIBUTE, mapper.writeValueAsString(suggestions));
        session.setAttribute(INDEX_ATTRIBUTE, 0);
        return response;
    }

    public SpeechletResponse handleGetArrivalTimeRequest(IntentRequest request,
                                                         Session session,
                                                         Intent intent,
                                                         TransitUser user)
            throws IOException {
        TransitSuggestion suggestion = getCurrentTransitSuggestion(session);

        if (suggestion == null) {
            return getTryAgainResponse(HELP_STRING);
        }

        DateTimeFormatter formatter = DateTimeFormat.forPattern(TIME_FORMAT)
                .withLocale(request.getLocale());

        String timezone =
                user.getTimeZone() == null ? DEFAULT_TIMEZONE : user.getTimeZone();

        String output = "You will arrive at " + suggestion.getArrivalTime()
                .withZone(DateTimeZone.forID(timezone))
                .toString(formatter);
        output = output.concat(".");
        String repromptQuestion = generateRepromptQuestion(intent, session);
        String finalOutput = output.concat(repromptQuestion);
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(finalOutput);
        SimpleCard card = new SimpleCard();
        card.setTitle("Arrival Time");
        card.setContent(output);
        session.setAttribute(PREVIOUS_RESPONSE_ATTRIBUTE, output);
        return SpeechletResponse.newAskResponse(outputSpeech,
                SpeechletUtils.getReprompt(repromptQuestion), card);
    }

    public SpeechletResponse handleGetTotalTransitDurationRequest(Session session,
                                                                  Intent intent) throws IOException {
        TransitSuggestion suggestion = getCurrentTransitSuggestion(session);

        if (suggestion == null) {
            return getTryAgainResponse(HELP_STRING);
        }
        Duration totalDuration = suggestion.getTotalDuration();

        if (totalDuration == null || totalDuration.humanReadable == null) {
            return getTryAgainResponse(ERROR_STRING);
        }
        log.info("Getting reprompt question.");
        String repromptQuestion = generateRepromptQuestion(intent, session);
        log.info("Obtained reprompt question: " + repromptQuestion);
        Reprompt reprompt = SpeechletUtils.getReprompt(repromptQuestion);
        String output = "It will take you " + totalDuration.humanReadable
                + " to arrive at your destination. ";
        String finalOutput = output + repromptQuestion;
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(finalOutput);
        SimpleCard card = new SimpleCard();
        card.setTitle("Transit Duration");
        card.setContent(output);
        session.setAttribute(PREVIOUS_RESPONSE_ATTRIBUTE, output);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt, card);
    }

    public SpeechletResponse handleGetDirectionsRequest(Session session,
                                                        Intent intent) throws IOException {
        TransitSuggestion suggestion = getCurrentTransitSuggestion(session);

        if (suggestion == null) {
            return getTryAgainResponse(HELP_STRING);
        }
        StringBuilder directions = new StringBuilder();
        String walkingDirections = suggestion.getWalkingInstruction();

        if (walkingDirections != null && !walkingDirections.isEmpty()) {
            directions.append(walkingDirections);
            directions.append(". After that, take the ");
        }
        String transitInstructions = suggestion.getTransitInstruction();

        if (transitInstructions == null || transitInstructions.isEmpty()) {
            return getTryAgainResponse(ERROR_STRING);
        }
        directions.append(transitInstructions);
        directions.append(".");
        String directionsOutput = directions.toString();

        String repromptQuestion = generateRepromptQuestion(intent, session);
        Reprompt reprompt = SpeechletUtils.getReprompt(repromptQuestion);

        directions.append(repromptQuestion);
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(directions.toString());
        SimpleCard card = new SimpleCard();
        card.setTitle("Transit Directions");
        card.setContent(directionsOutput);
        session.setAttribute(PREVIOUS_RESPONSE_ATTRIBUTE, directionsOutput);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt, card);
    }

    public SpeechletResponse handleRepeatSuggestionRequest(Session session, Intent intent)
            throws IOException {
        String previousResponse = (String) session.getAttribute(PREVIOUS_RESPONSE_ATTRIBUTE);

        if (previousResponse == null) {
            return getTryAgainResponse(HELP_STRING);
        }
        String repromptQuestion = generateRepromptQuestion(intent, session);
        Reprompt reprompt = SpeechletUtils.getReprompt(repromptQuestion);
        String finalResponse = previousResponse.concat(repromptQuestion);

        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(finalResponse);
        SimpleCard card = new SimpleCard();
        card.setContent(previousResponse);
        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    public SpeechletResponse handleNextSuggestionRequest(Session session, Intent intent)
            throws IOException {
        TransitSuggestion suggestion;

        try {
            suggestion = getTransitSuggestionFromSession(session, 1);
        } catch (IndexOutOfBoundsException ex) {
            return getNoMoreTransitOptionsResposne();
        }
        return suggestionToDetailedResponse(suggestion, session, "Your next option is ", intent);
    }

    public SpeechletResponse handlePreviousSuggestionRequest(Session session, Intent intent)
            throws IOException {
        TransitSuggestion suggestion;

        try {
            suggestion = getTransitSuggestionFromSession(session, -1);
        } catch (IndexOutOfBoundsException ex) {
            return getNoMoreTransitOptionsResposne();
        }
        return suggestionToDetailedResponse(suggestion, session,
                "The previous option was ", intent);
    }

    public SpeechletResponse getTryAgainResponse(String returnSpeech) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(returnSpeech);

        SimpleCard card = new SimpleCard();
        card.setTitle("Try again");
        card.setContent(returnSpeech);

        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt, card);
    }

    public SpeechletResponse handleYesNoIntentResponse(Session session,
                                                       Intent intent,
                                                       IntentRequest request,
                                                       TransitUser user)
            throws IOException {
        String intentName = intent.getName();

        if (intentName.equals("YesIntent") &&
            session.getAttributes().containsKey(REPROMPT_INTENT_ATTRIBUTE) &&
            session.getAttributes().containsKey(SUGGESTION_ATTRIBUTE)) {
            String repromptIntent = (String)session.getAttribute(REPROMPT_INTENT_ATTRIBUTE);

            switch (repromptIntent) {
                case "GetArrivalTime":
                    return handleGetArrivalTimeRequest(request, session, intent, user);

                case "GetTotalTransitDuration":
                    return handleGetTotalTransitDurationRequest(session, intent);

                case "GetDirections":
                    return handleGetDirectionsRequest(session, intent);

                case "AMAZON.RepeatIntent":
                    return handleRepeatSuggestionRequest(session, intent);

                case "AMAZON.NextIntent":
                    return handleNextSuggestionRequest(session, intent);

                case "AMAZON.PreviousIntent":
                    return handlePreviousSuggestionRequest(session, intent);

                default:
                    return getErrorResponse(ERROR_STRING);
            }

        } else if (intentName.equals("NoIntent")) {
            return SpeechletUtils.getNewTellResponse("Bye. Have a nice ride. ",
                    "Have a safe ride.");
        } else {
            return getErrorResponse(ERROR_STRING);
        }
    }

    private SpeechletResponse getErrorResponse(String errorText) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(errorText);
        return SpeechletResponse.newTellResponse(outputSpeech);
    }

    private SpeechletResponse suggestionToDetailedResponse(TransitSuggestion suggestion,
                                                           Session session, String introText,
                                                           Intent intent) {
        StringBuilder outputSpeechBuilder = new StringBuilder();
        String transitType = suggestion.getTransitType();

        log.info("Retrieved Transit Suggestion: \n" + suggestion.toString());

        if (introText != null) {
            outputSpeechBuilder.append(introText);
        }

        if (suggestion.getTransitId() != null) {
            outputSpeechBuilder.append(transitType + " number "
                    + suggestion.getTransitId() +". ");
        }
        int minutesToTransitArrival = suggestion.getTimeToTransitArrivalInMinutes();
        String minutesString = minutesToTransitArrival == 1 ? "minute" : "minutes";

        outputSpeechBuilder.append("It will arrive in "
                + minutesToTransitArrival + " " + minutesString + ". ");

        if (suggestion.getWalkingDuration() != null) {
            outputSpeechBuilder.append("It will take you " +
                    suggestion.getWalkingDuration().humanReadable +
                    " to walk to the " + transitType + " location. ");
        }
        int leavingTimeSeconds = suggestion.getLeavingTimeInSeconds();

        if (leavingTimeSeconds <= 100) {
            outputSpeechBuilder.append("I recommend you leave now. ");
        } else {
            int leavingTimeMinutes = leavingTimeSeconds / 60;
            minutesString = leavingTimeMinutes == 1 ? "minute" : "minutes";
            outputSpeechBuilder.append("You should leave in " +
                    leavingTimeMinutes + " " + minutesString + ". ");
        }

        int numSwitches = suggestion.getNumOfSwitches();

        if (numSwitches > 0) {
            if (numSwitches == 1) {
                outputSpeechBuilder.append("You will have to make a transit switch. ");
            } else {
                outputSpeechBuilder.append("You will have to make " +
                        suggestion.getNumOfSwitches() + " transit switches. ");
            }
        }
        String suggestionOutput = outputSpeechBuilder.toString();
        log.info("Generating reprompt question");
        String repromptQuestion = generateRepromptQuestion(intent, session);
        log.info("Random reprompt question generated: " + repromptQuestion);
        outputSpeechBuilder.append(repromptQuestion);
        Reprompt reprompt = SpeechletUtils.getReprompt(repromptQuestion);

        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(outputSpeechBuilder.toString());

        SimpleCard card = new SimpleCard();
        card.setTitle("Transit Suggestion");
        card.setContent(suggestionOutput);
        session.setAttribute(PREVIOUS_RESPONSE_ATTRIBUTE, suggestionOutput);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt, card);
    }

    private SpeechletResponse getNoMoreTransitOptionsResposne() {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        String output = "Sorry. No more transit options available. ";
        outputSpeech.setText(output);
        SimpleCard card = new SimpleCard();
        card.setTitle("Transit Suggestion");
        card.setContent(output);
        return SpeechletResponse.newTellResponse(outputSpeech, card);
    }

    private TransitSuggestion getCurrentTransitSuggestion(Session session) throws IOException {
        TransitSuggestion suggestion = getTransitSuggestionFromSession(session, 0);
        return suggestion;
    }

    private TransitSuggestion getTransitSuggestionFromSession(Session session, int indexAdd) throws IOException {
        Map<String, Object> attributes = session.getAttributes();

        if (!attributes.containsKey(INDEX_ATTRIBUTE) ||
                !attributes.containsKey(SUGGESTION_ATTRIBUTE)) {
            return null;
        }
        String suggestionsText = (String) session.getAttribute(SUGGESTION_ATTRIBUTE);
        List<TransitSuggestion> suggestions = mapper.readValue(suggestionsText,
                new TypeReference<List<TransitSuggestion>>(){});

        int idx = (Integer) session.getAttribute(INDEX_ATTRIBUTE);
        idx += indexAdd;

        if (idx < 0 || idx >= suggestions.size()) {
            String err = "Transit Suggestion not available at index " + idx;
            log.warn(err);
            throw new IndexOutOfBoundsException(err);
        }
        log.info("Setting Session Attribute: " +
                INDEX_ATTRIBUTE+" to index: " + idx);
        session.setAttribute(INDEX_ATTRIBUTE, idx);
        TransitSuggestion suggestion = suggestions.get(idx);
        return suggestion;
    }

    /**
     * Gets a random reprompt based on the current intent
     * Removes the possibility of repeating the reprompt
     * mapped to the current intent. This improves the user
     * experience since alexa will not reprompt the same
     * question that she just answered.
     */
    private String generateRepromptQuestion(Intent intent,
                                            Session session) {
        Map<String, Object> attributes = session.getAttributes();
        String intentName = intent.getName();

        if (intentName.equals("YesIntent")) {
            intentName = (String) attributes.get(REPROMPT_INTENT_ATTRIBUTE);
        }
        Map<String, String> repromptMap = new HashMap<>(REPROMPT_QUESTIONS);

        if (repromptMap.containsKey(intentName)) {
            // Return a reprompt question that isn't
            repromptMap.remove(intentName);
        }

        // Remove NextOption reprompt if no more next suggestions exist
        if (!nextSuggestionExists(session)) {
            repromptMap.remove("AMAZON.NextIntent");
        }

        // Remove PreviousOption reprompt if no more previous suggestions exist
        if (!previousSuggestionExists(session)) {
            repromptMap.remove("AMAZON.PreviousIntent");
        }
        String randomIntent = getRandomKey(repromptMap);
        session.setAttribute(REPROMPT_INTENT_ATTRIBUTE, randomIntent);
        String repromptQuestion = repromptMap.get(randomIntent);
        return repromptQuestion;
    }

    private boolean nextSuggestionExists(Session session) {
        Map<String, Object> attributes = session.getAttributes();

        if (attributes == null) {
            return false;
        }

        try {
            if (attributes.containsKey(SUGGESTION_ATTRIBUTE)) {
                String suggestionsText = (String) session.getAttribute(SUGGESTION_ATTRIBUTE);
                List<TransitSuggestion> suggestions = mapper.readValue(suggestionsText,
                        new TypeReference<List<TransitSuggestion>>(){});
                int idx = (Integer) session.getAttribute(INDEX_ATTRIBUTE);

                if (idx >= suggestions.size() - 1) {
                    return false;
                }
                return true;
            }
        } catch (Exception ex) {
            log.error(ex);
            return false;
        }
        return false;
    }

    private boolean previousSuggestionExists(Session session) {
        Map<String, Object> attributes = session.getAttributes();

        if (attributes == null) {
            return false;
        }

        try {
            if (attributes.containsKey(SUGGESTION_ATTRIBUTE)) {
                int idx = (Integer) session.getAttribute(INDEX_ATTRIBUTE);

                if (idx <= 0) {
                    return false;
                }
                return true;
            }
        } catch (Exception ex) {
            log.error(ex);
            return false;
        }
        return false;
    }

    private String getRandomKey(Map<String, String> map) {
        List<String> keys = new ArrayList<>(map.keySet());
        Random random = new Random();
        int idx = random.nextInt(keys.size());
        return keys.get(idx);
    }
}
