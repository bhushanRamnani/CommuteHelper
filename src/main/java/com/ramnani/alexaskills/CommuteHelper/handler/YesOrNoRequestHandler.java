package com.ramnani.alexaskills.CommuteHelper.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.impl.IntentRequestHandler;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitUser;
import com.ramnani.alexaskills.CommuteHelper.TransitSpeechletManager;
import com.ramnani.alexaskills.CommuteHelper.UserSetupSpeechletManager;
import com.ramnani.alexaskills.CommuteHelper.util.AlexaUtils;
import com.ramnani.alexaskills.CommuteHelper.util.Validator;
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
        Validator.validateHandlerInput(input);
        Validator.validateIntentRequest(intentRequest);

        Optional<TransitUser> transitUser = userSetupSpeechletManager.getTransitUser(input);

        if (!transitUser.isPresent()) {
            log.info("Transit user does not exist. Going through user setup: " + AlexaUtils.getUserId(input));
            return userSetupSpeechletManager.handleUserSetup(input, intentRequest.getIntent());
        }

        Map<String, Object> sessionAttributes = input.getAttributesManager().getSessionAttributes();

        if (sessionAttributes.containsKey(TransitSpeechletManager.SUGGESTION_ATTRIBUTE)) {
            // User is in a Transit Suggestion related session
            log.info("Handling suggestion.");
            return transitSpeechletManager
                    .handleYesNoIntentResponse(input, intentRequest, transitUser.get());
        } else if (sessionAttributes.containsKey(UserSetupSpeechletManager.SETUP_ATTRIBUTE)) {
            // User is in a Setup session
            log.info("Handling address setup");
            return userSetupSpeechletManager
                    .handleVerifyPostalAddressRequest(input, intentRequest.getIntent());
        }
        return AlexaUtils.getInternalServerErrorResponse(input);
    }
}
