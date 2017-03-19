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
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
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

    private static final String SUGGESTION_ATTRIBUTE = "suggestion";

    private static final String INDEX_ATTRIBUTE = "index";

    private static final String PREVIOUS_RESPONSE_ATTRIBUTE = "previousResponse";

    private static final String HELP_STRING = "Sorry. I don't know that. " +
            "First, ask me for a transit suggestion. " +
            "For example, ask me when's my next bus";

    private static final String ERROR_STRING = "Sorry. I'm having some issues " +
            "giving you an answer right now.";

    private static final String[] REPROMPT_RESPONSES = {
            "If you wish to know the arrival time, you can ask me, what is the arrival time.",
            "If you wish to get the travel duration, you can ask me, \"how long will it take.\"",
            "If you'd like to get directions, you can ask me, \"can I get directions.\"",
            "If you'd like to get information on alternate routes, you can say, \"Next Option.\"",
            "If you'd like me to repeat this information, you can say, \"Repeat.\""
    };

    private static final String TIME_FORMAT = "hh:mm a";

    private ObjectMapper mapper = new ObjectMapper();

    private GoogleMapsService googleMapsService;

    public TransitSpeechletManager(GoogleMapsService googleMapsService) {
        Validate.notNull(googleMapsService);
        this.googleMapsService = googleMapsService;
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
                "Your next " + transitType + " is ");
        session.setAttribute(SUGGESTION_ATTRIBUTE, mapper.writeValueAsString(suggestions));
        session.setAttribute(INDEX_ATTRIBUTE, 0);
        return response;
    }

    public SpeechletResponse handleGetArrivalTimeRequest(IntentRequest request, Session session) throws IOException {
        TransitSuggestion suggestion = getCurrentTransitSuggestion(session);

        if (suggestion == null) {
            return getTryAgainResponse(HELP_STRING);
        }

        DateTimeFormatter formatter = DateTimeFormat.forPattern(TIME_FORMAT)
                .withLocale(request.getLocale());

        String output = "You will arrive at " + suggestion.getArrivalTime()
                .withZone(DateTimeZone.forID("America/Los_Angeles"))
                .toString(formatter);

        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(output);
        SimpleCard card = new SimpleCard();
        card.setTitle("Arrival Time");
        card.setContent(output);
        session.setAttribute(PREVIOUS_RESPONSE_ATTRIBUTE, output);
        return SpeechletResponse.newAskResponse(outputSpeech, getRandomReprompt(), card);
    }

    public SpeechletResponse handleGetTotalTransitDurationRequest(Session session) throws IOException {
        TransitSuggestion suggestion = getCurrentTransitSuggestion(session);

        if (suggestion == null) {
            return getTryAgainResponse(HELP_STRING);
        }
        Duration totalDuration = suggestion.getTotalDuration();

        if (totalDuration == null || totalDuration.humanReadable == null) {
            return getTryAgainResponse(ERROR_STRING);
        }

        String output = "It will take you " + totalDuration.humanReadable
                + " to arrive at your destination.";
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(output);
        SimpleCard card = new SimpleCard();
        card.setTitle("Transit Duration");
        card.setContent(output);
        session.setAttribute(PREVIOUS_RESPONSE_ATTRIBUTE, output);
        return SpeechletResponse.newAskResponse(outputSpeech, getRandomReprompt(), card);
    }

    public SpeechletResponse handleGetDirectionsRequest(Session session) throws IOException {
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
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(directions.toString());
        SimpleCard card = new SimpleCard();
        card.setTitle("Transit Directions");
        card.setContent(directions.toString());
        session.setAttribute(PREVIOUS_RESPONSE_ATTRIBUTE, directions.toString());
        return SpeechletResponse.newAskResponse(outputSpeech, getRandomReprompt(), card);
    }

    public SpeechletResponse handleRepeatSuggestionRequest(Session session)
            throws IOException {
        String previousResponse = (String) session.getAttribute(PREVIOUS_RESPONSE_ATTRIBUTE);

        if (previousResponse == null) {
            return getTryAgainResponse(HELP_STRING);
        }
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(previousResponse);
        SimpleCard card = new SimpleCard();
        card.setContent(previousResponse);
        return SpeechletResponse.newAskResponse(speech, getRandomReprompt(), card);
    }

    public SpeechletResponse handleNextSuggestionRequest(Session session)
            throws IOException {
        TransitSuggestion suggestion;

        try {
            suggestion = getTransitSuggestionFromSession(session, 1);
        } catch (IndexOutOfBoundsException ex) {
            return getNoMoreTransitOptionsResposne();
        }
        return suggestionToDetailedResponse(suggestion, session, "Your next option is ");
    }

    public SpeechletResponse handlePreviousSuggestionRequest(Session session)
            throws IOException {
        TransitSuggestion suggestion;

        try {
            suggestion = getTransitSuggestionFromSession(session, -1);
        } catch (IndexOutOfBoundsException ex) {
            return getNoMoreTransitOptionsResposne();
        }
        return suggestionToDetailedResponse(suggestion, session, "The previous option was ");
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

    private SpeechletResponse getErrorResponse(String errorText) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(errorText);
        return SpeechletResponse.newTellResponse(outputSpeech);
    }

    private SpeechletResponse suggestionToDetailedResponse(TransitSuggestion suggestion,
                                                           Session session, String introText) {
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
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(outputSpeechBuilder.toString());

        SimpleCard card = new SimpleCard();
        card.setTitle("Transit Suggestion");
        card.setContent(outputSpeechBuilder.toString());
        session.setAttribute(PREVIOUS_RESPONSE_ATTRIBUTE, outputSpeechBuilder.toString());
        return SpeechletResponse.newAskResponse(outputSpeech, getRandomReprompt(), card);
    }

    private SpeechletResponse getNoMoreTransitOptionsResposne() {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        String output = "Sorry. No more transit options available. ";
        outputSpeech.setText(output);
        SimpleCard card = new SimpleCard();
        card.setTitle("Transit Suggestion");
        card.setContent(output);
        return SpeechletResponse.newAskResponse(outputSpeech, getRandomReprompt(), card);
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

    private Reprompt getRandomReprompt() {
        Reprompt reprompt = new Reprompt();
        Random random = new Random();
        int index = random.nextInt(REPROMPT_RESPONSES.length);
        String repromptText = REPROMPT_RESPONSES[index];
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(repromptText);
        reprompt.setOutputSpeech(speech);
        return reprompt;
    }
}
