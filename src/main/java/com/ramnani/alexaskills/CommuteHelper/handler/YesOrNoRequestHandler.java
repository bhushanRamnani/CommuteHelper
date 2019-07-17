package com.ramnani.alexaskills.CommuteHelper.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import com.amazon.ask.response.ResponseBuilder;
import com.ramnani.alexaskills.CommuteHelper.CommuteHelperSpeechlet;
import com.ramnani.alexaskills.CommuteHelper.TransitSpeechletManager;
import com.ramnani.alexaskills.CommuteHelper.UserSetupSpeechletManager;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Optional;

public class YesOrNoRequestHandler implements IntentRequestHandler {

    private static final Logger log = Logger.getLogger(YesOrNoRequestHandler.class);

    private TransitSpeechletManager transitSpeechletManager;
    private UserSetupSpeechletManager userSetupSpeechletManager;

    public YesOrNoRequestHandler(TransitSpeechletManager transitSpeechletManager,
                                 UserSetupSpeechletManager userSetupSpeechletManager) {
        this.transitSpeechletManager = transitSpeechletManager;
        this.userSetupSpeechletManager = userSetupSpeechletManager;
    }

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName("YesIntent")
                .or(Predicates.intentName("NoIntent")));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Map<String, Object> sessionAttributes = input.getAttributesManager().getSessionAttributes();

        if (sessionAttributes.containsKey(TransitSpeechletManager.SUGGESTION_ATTRIBUTE)) {
            // User is in a Transit Suggestion related session
            log.info("Handling suggestion.");
            return transitSpeechletManager
                    .handleYesNoIntentResponse(session, intent, request, user);
        } else if (sessionAttributes.containsKey(UserSetupSpeechletManager.SETUP_ATTRIBUTE)) {
            // User is in a Setup session
            log.info("Handling address setup");
            return userSetupSpeechletManager
                    .handleVerifyPostalAddressRequest(session, intent);
        }
        return getInternalServerErrorResponse();

        return input.getResponseBuilder()
                .withSpeech(speechText)
                .withSimpleCard("Transit Helper", speechText)
                .withReprompt(speechText)
                .build();
    }

    @Override
    public boolean canHandle(HandlerInput input) {
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {

    }
}
