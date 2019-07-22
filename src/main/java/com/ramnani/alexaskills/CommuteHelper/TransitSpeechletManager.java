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
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;

import com.amazon.ask.model.Slot;
import com.amazon.ask.response.ResponseBuilder;
import com.google.maps.model.Duration;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitUser;
import com.ramnani.alexaskills.CommuteHelper.util.Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
        REPROMPT_QUESTIONS.put("AMAZON.PreviousIntent",
                               " Would you like to hear the previous option again?");
        REPROMPT_QUESTIONS.put("AMAZON.RepeatIntent",
                               " Would you like me to repeat this option?");
    }

    public Optional<Response> handleNextTransitRequest(Intent intent,
                                                       TransitUser user,
                                                       HandlerInput handlerInput) {
        Validator.validateHandlerInput(handlerInput);
        Validator.validateIntent(intent);
        Validate.notNull(user);

        log.info("Inside handleNextTransitRequest. Request: " + handlerInput.getRequestEnvelope()
                + ". Intent: " + intent);

        Slot slot = intent.getSlots().get(SLOT_TRANSIT);
        String transitType = slot.getValue();
        String homeAddress = user.getHomeAddress();
        String requestId = handlerInput.getRequest().getRequestId();
        Map<String, Object> sessionAttributes =
                handlerInput.getAttributesManager().getSessionAttributes();

        if (homeAddress == null || homeAddress.isEmpty()) {
            log.error("Sorry. Home Address does not exist for user: " + user.getUserId());

            return handlerInput.getResponseBuilder()
                    .withSpeech("Home Address does not exist.")
                    .withShouldEndSession(true)
                    .build();
        }

        if (StringUtils.isBlank(transitType)) {
            log.info("Transit type not provided with session: " + requestId);
            String output = "Please specify your preferred mode of transport. For example, you can ask,"
                    + " When's the next bus to work ?";

            return handlerInput.getResponseBuilder()
                    .withReprompt(output)
                    .withSpeech(output)
                    .build();

        }
        Map<String, String> destinations = user.getDestinations();
        log.info("Destinations : " + destinations.toString() + ". User: " + user.getUserId());

        if (destinations == null || !destinations.containsKey(WORK_KEY)) {
            log.error("Sorry. Work address does not exist for user: " + user.getUserId());

            return handlerInput.getResponseBuilder()
                    .withSpeech("Work address does not exist")
                    .withShouldEndSession(true)
                    .build();
        }
        String workAddress = destinations.get(WORK_KEY);

        List<TransitSuggestion> suggestions = googleMapsService
                        .getNextTransitToDestination(transitType, homeAddress, workAddress);

        if (suggestions == null || suggestions.size() == 0) {
            log.warn("No Suggestions for user: " + user.getUserId());
            String speechText =
                    "Sorry. There are no available transit options " +
                            "for your destination at this time.";
            return handlerInput.getResponseBuilder()
                    .withSpeech(speechText)
                    .withShouldEndSession(true)
                    .build();
        }

        try {
            sessionAttributes.put(SUGGESTION_ATTRIBUTE, mapper.writeValueAsString(suggestions));
        } catch (Exception ex) {
            log.error("Failed to parse suggestions: " + suggestions, ex);
            throw new IllegalStateException("Failed to parse suggestions: " + suggestions);
        }
        sessionAttributes.put(INDEX_ATTRIBUTE, 0);
        TransitSuggestion suggestion = suggestions.get(0);
        Optional<Response> response = suggestionToDetailedResponse(suggestion, handlerInput,
                "Your next " + transitType + " is ", intent);
        return response;
    }

    public Optional<Response> handleGetArrivalTimeRequest(IntentRequest request,
                                                          HandlerInput handlerInput,
                                                          TransitUser user) {
        Validator.validateIntentRequest(request);
        Validator.validateHandlerInput(handlerInput);

        log.info("Inside handleGetArrivalTimeRequest. Request: " + handlerInput.getRequestEnvelope()
                + " Intent: " + request.getIntent());

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager()
                .getSessionAttributes();
        TransitSuggestion suggestion = getCurrentTransitSuggestion(sessionAttributes);

        if (suggestion == null) {
            return getTryAgainResponse(HELP_STRING, handlerInput.getResponseBuilder());
        }

        Locale locale = null;

        try {
            locale = Locale.forLanguageTag(request.getLocale());
        } catch (Exception ex) {
            log.error("Failed to get locale from request: " + request, ex);
            locale = Locale.US;
        }
        DateTimeFormatter formatter = DateTimeFormat.forPattern(TIME_FORMAT)
                .withLocale(locale);

        String timezone =
                user.getTimeZone() == null ? DEFAULT_TIMEZONE : user.getTimeZone();

        StringBuilder arrivalTimeOutput = new StringBuilder();
        String output = "You will arrive at " + suggestion.getArrivalTime()
                .withZone(DateTimeZone.forID(timezone))
                .toString(formatter);
        arrivalTimeOutput.append(output + ".");
        Intent intent = request.getIntent();
        return addRepromptQuestionAndReturnResponse(arrivalTimeOutput,
                "Arrival Time", handlerInput, intent);
    }

    public Optional<Response> handleGetTotalTransitDurationRequest(HandlerInput handlerInput,
                                                                   Intent intent) {
        Validator.validateIntent(intent);
        Validator.validateHandlerInput(handlerInput);

        log.info("Inside handleGetTotalTransitDurationRequest. Request: " + handlerInput.getRequestEnvelope()
                + " Intent: " + intent);

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager()
                .getSessionAttributes();
        TransitSuggestion suggestion = getCurrentTransitSuggestion(sessionAttributes);

        if (suggestion == null) {
            return getTryAgainResponse(HELP_STRING, handlerInput.getResponseBuilder());
        }
        Duration totalDuration = suggestion.getTotalDuration();

        if (totalDuration == null || totalDuration.humanReadable == null) {
            return getTryAgainResponse(ERROR_STRING, handlerInput.getResponseBuilder());
        }
        StringBuilder durationOutput = new StringBuilder();
        String output = "It will take you " + totalDuration.humanReadable
                + " to arrive at your destination. ";
        durationOutput.append(output);
        return addRepromptQuestionAndReturnResponse(durationOutput,
                "Transit Duration", handlerInput, intent);
    }

    public Optional<Response> handleGetDirectionsRequest(HandlerInput handlerInput,
                                                         Intent intent) {
        Validator.validateIntent(intent);
        Validator.validateHandlerInput(handlerInput);

        log.info("Inside handleGetDirectionsRequest. Request: " + handlerInput.getRequestEnvelope()
                + " Intent: " + intent);

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager()
                .getSessionAttributes();

        TransitSuggestion suggestion = getCurrentTransitSuggestion(sessionAttributes);

        if (suggestion == null) {
            return getTryAgainResponse(HELP_STRING, handlerInput.getResponseBuilder());
        }
        StringBuilder directions = new StringBuilder();
        String walkingDirections = suggestion.getWalkingInstruction();

        if (walkingDirections != null && !walkingDirections.isEmpty()) {
            directions.append(walkingDirections);
            directions.append(". After that, take the ");
        }
        String transitInstructions = suggestion.getTransitInstruction();

        if (transitInstructions == null || transitInstructions.isEmpty()) {
            return getTryAgainResponse(ERROR_STRING, handlerInput.getResponseBuilder());
        }
        directions.append(transitInstructions + ". ");
        return addRepromptQuestionAndReturnResponse(directions,
                "Transit Directions", handlerInput, intent);
    }

    public Optional<Response> handleRepeatSuggestionRequest(HandlerInput handlerInput,
                                                            Intent intent) {
        Validator.validateIntent(intent);
        Validator.validateHandlerInput(handlerInput);

        log.info("Inside handleRepeatSuggestionRequest. Request: " + handlerInput.getRequestEnvelope()
                + " Intent: " + intent);

        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager()
                .getSessionAttributes();
        String previousResponse = (String) sessionAttributes.get(PREVIOUS_RESPONSE_ATTRIBUTE);

        if (previousResponse == null) {
            return getTryAgainResponse(HELP_STRING, handlerInput.getResponseBuilder());
        }
        StringBuilder repeatSuggestionOutput = new StringBuilder();
        repeatSuggestionOutput.append(previousResponse);
        return addRepromptQuestionAndReturnResponse(repeatSuggestionOutput,
                "Previous Suggestion", handlerInput, intent);

    }

    public Optional<Response> handleNextSuggestionRequest(HandlerInput handlerInput,
                                                          Intent intent) {
        Validator.validateIntent(intent);
        Validator.validateHandlerInput(handlerInput);

        log.info("Inside handleNextSuggestionRequest. Request: " + handlerInput.getRequestEnvelope()
                + " Intent: " + intent);

        TransitSuggestion suggestion;

        try {
            suggestion = getTransitSuggestionFromSession(handlerInput
                    .getAttributesManager().getSessionAttributes(), 1);
        } catch (IndexOutOfBoundsException ex) {
            return getNoMoreTransitOptionsResposne(handlerInput.getResponseBuilder());
        }
        return suggestionToDetailedResponse(suggestion, handlerInput,
                "Your next option is ", intent);
    }

    public Optional<Response> handlePreviousSuggestionRequest(HandlerInput handlerInput,
                                                              Intent intent) {
        Validator.validateIntent(intent);
        Validator.validateHandlerInput(handlerInput);

        log.info("Inside handlePreviousSuggestionRequest. Request: " + handlerInput.getRequestEnvelope()
                + " Intent: " + intent);

        TransitSuggestion suggestion;

        try {
            suggestion = getTransitSuggestionFromSession(handlerInput
                    .getAttributesManager().getSessionAttributes(), -1);
        } catch (IndexOutOfBoundsException ex) {
            return getNoMoreTransitOptionsResposne(handlerInput.getResponseBuilder());
        }
        return suggestionToDetailedResponse(suggestion, handlerInput,
                "The previous option was ", intent);
    }

    private Optional<Response> getTryAgainResponse(String returnSpeech,
                                                   ResponseBuilder responseBuilder) {

        return responseBuilder.withSpeech(returnSpeech)
                .withReprompt(returnSpeech)
                .withSimpleCard("Try Again", returnSpeech)
                .build();
    }

    public Optional<Response> handleYesNoIntentResponse(HandlerInput handlerInput,
                                                        IntentRequest request,
                                                        TransitUser user) {
        Validator.validateHandlerInput(handlerInput);
        Validator.validateIntentRequest(request);

        log.info("Inside handleYesNoIntentResponse. Request: " + handlerInput.getRequestEnvelope()
                + " Intent: " + request.getIntent());

        Intent intent = request.getIntent();
        String intentName = intent.getName();
        Map<String, Object> sessionAttributes = handlerInput.getAttributesManager()
                .getSessionAttributes();

        if (intentName.equals("YesIntent") &&
            sessionAttributes.containsKey(REPROMPT_INTENT_ATTRIBUTE) &&
            sessionAttributes.containsKey(SUGGESTION_ATTRIBUTE)) {
            String repromptIntent = (String) sessionAttributes.get(REPROMPT_INTENT_ATTRIBUTE);

            switch (repromptIntent) {
                case "GetArrivalTime":
                    return handleGetArrivalTimeRequest(request, handlerInput, user);

                case "GetTotalTransitDuration":
                    return handleGetTotalTransitDurationRequest(handlerInput, intent);

                case "GetDirections":
                    return handleGetDirectionsRequest(handlerInput, intent);

                case "AMAZON.RepeatIntent":
                    return handleRepeatSuggestionRequest(handlerInput, intent);

                case "AMAZON.NextIntent":
                    return handleNextSuggestionRequest(handlerInput, intent);

                case "AMAZON.PreviousIntent":
                    return handlePreviousSuggestionRequest(handlerInput, intent);

                default:
                    return handlerInput.getResponseBuilder()
                            .withSpeech(ERROR_STRING)
                            .withShouldEndSession(true)
                            .build();
            }

        } else if (intentName.equals("NoIntent")) {
            return handlerInput.getResponseBuilder()
                    .withSpeech("Bye. Have a nice ride. ")
                    .withShouldEndSession(true)
                    .withSimpleCard("Have a safe ride.", "Bye. Have a nice ride. ")
                    .build();
        } else {
            return handlerInput.getResponseBuilder()
                    .withSpeech(ERROR_STRING)
                    .withShouldEndSession(true)
                    .build();
        }
    }

    private Optional<Response> suggestionToDetailedResponse(TransitSuggestion suggestion,
                                                            HandlerInput handlerInput,
                                                            String introText,
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
        return addRepromptQuestionAndReturnResponse(outputSpeechBuilder,
                "Transit Suggestion", handlerInput, intent);
    }

    private Optional<Response> getNoMoreTransitOptionsResposne(ResponseBuilder responseBuilder) {
        String output = "Sorry. No more transit options available. ";
        return responseBuilder.withSpeech(output)
                .withSimpleCard("Transit Suggestion", output)
                .build();
    }

    private TransitSuggestion getCurrentTransitSuggestion(Map<String, Object> sessionAttributes) {
        TransitSuggestion suggestion = getTransitSuggestionFromSession(sessionAttributes, 0);
        return suggestion;
    }

    private TransitSuggestion getTransitSuggestionFromSession(Map<String, Object> sessionAttributes,
                                                              int indexAdd) {

        if (!sessionAttributes.containsKey(INDEX_ATTRIBUTE) ||
                !sessionAttributes.containsKey(SUGGESTION_ATTRIBUTE)) {
            return null;
        }
        String suggestionsText = (String) sessionAttributes.get(SUGGESTION_ATTRIBUTE);

        List<TransitSuggestion> suggestions = null;

        try {
            suggestions = mapper.readValue(suggestionsText,
                    new TypeReference<List<TransitSuggestion>>(){});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse suggestions: " + suggestionsText, ex);
        }

        int idx = (Integer) sessionAttributes.get(INDEX_ATTRIBUTE);
        idx += indexAdd;

        if (idx < 0 || idx >= suggestions.size()) {
            String err = "Transit Suggestion not available at index " + idx;
            log.warn(err);
            throw new IndexOutOfBoundsException(err);
        }
        log.info("Setting Session Attribute: " +
                INDEX_ATTRIBUTE+" to index: " + idx);
        sessionAttributes.put(INDEX_ATTRIBUTE, idx);
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
    private String generateRepromptQuestion(Map<String, Object> sessionAttributes, Intent intent) {
        Map<String, String> repromptMap = new HashMap<>(REPROMPT_QUESTIONS);

        String intentName = intent.getName();

        if (intentName.equals("YesIntent")) {
            intentName = (String) sessionAttributes.get(REPROMPT_INTENT_ATTRIBUTE);
        }

        String[] nextOptionIntents = { "AMAZON.NextIntent",
                                       "AMAZON.PreviousIntent",
                                       "AMAZON.RepeatIntent",
                                       "GetNextTransitToWork" };

        // Always give the next option if the current intent is one of the above
        if (nextSuggestionExists(sessionAttributes) &&
            Arrays.asList(nextOptionIntents).contains(intentName))
        {
            sessionAttributes.put(REPROMPT_INTENT_ATTRIBUTE, "AMAZON.NextIntent");
            return " Would you like to hear the next option?";
        }

        // Remove PreviousOption reprompt if no more previous suggestions exist
        if (!previousSuggestionExists(sessionAttributes)) {
            repromptMap.remove("AMAZON.PreviousIntent");
        }

        if (repromptMap.containsKey(intentName)) {
            // Return a reprompt question that doesn't reprompt for the current intent.
            repromptMap.remove(intentName);
        }

        String randomIntent = getRandomKey(repromptMap);
        sessionAttributes.put(REPROMPT_INTENT_ATTRIBUTE, randomIntent);
        return repromptMap.get(randomIntent);
    }

    private Optional<Response> addRepromptQuestionAndReturnResponse(
            StringBuilder stringBuilder, String cardTitle, HandlerInput handlerInput, Intent intent) {
        String actualOutput = stringBuilder.toString();
        actualOutput = actualOutput.replace("&", "and");
        stringBuilder.append("<break time=\"1s\"/>");

        Map<String, Object> session = handlerInput.getAttributesManager()
                .getSessionAttributes();

        String repromptQuestion = generateRepromptQuestion(session, intent);

        session.put(PREVIOUS_RESPONSE_ATTRIBUTE, actualOutput);

        return handlerInput.getResponseBuilder()
                .withSpeech("<speak>" + stringBuilder.toString()
                        .replace("&", "and") + "</speak>")
                .withReprompt(repromptQuestion)
                .withSimpleCard(cardTitle, actualOutput)
                .build();
    }

    private boolean nextSuggestionExists(Map<String, Object> sessionAttributes) {
        if (sessionAttributes == null) {
            return false;
        }

        try {
            if (sessionAttributes.containsKey(SUGGESTION_ATTRIBUTE)) {
                String suggestionsText = (String) sessionAttributes.get(SUGGESTION_ATTRIBUTE);
                List<TransitSuggestion> suggestions = mapper.readValue(suggestionsText,
                        new TypeReference<List<TransitSuggestion>>(){});
                int idx = (Integer) sessionAttributes.get(INDEX_ATTRIBUTE);

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

    private boolean previousSuggestionExists(Map<String, Object> sessionAttributes) {
        if (sessionAttributes == null) {
            return false;
        }

        try {
            if (sessionAttributes.containsKey(SUGGESTION_ATTRIBUTE)) {
                int idx = (Integer) sessionAttributes.get(INDEX_ATTRIBUTE);

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
