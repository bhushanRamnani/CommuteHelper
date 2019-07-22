package com.ramnani.alexaskills.CommuteHelper.util;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import org.apache.commons.lang3.Validate;

public final class Validator {

    private Validator() {
    }

    public static void validateIntentRequest(IntentRequest intentRequest) {
        Validate.notNull(intentRequest, "intentRequest was null.");
        validateIntent(intentRequest.getIntent());
    }

    public static void validateIntent(Intent intent) {
        Validate.notNull(intent, "intent was null.");
        Validate.notBlank(intent.getName(), "intent.getName() name was null");
    }

    public static void validateHandlerInput(HandlerInput handlerInput) {
        Validate.notNull(handlerInput, "handlerInput was null.");
        Validate.notNull(handlerInput.getAttributesManager(),
                "handlerInput.getAttributesManager() was null.");

        Validate.notNull(handlerInput.getAttributesManager().getSessionAttributes(),
                "handlerInput.getAttributesManager().getSessionAttributes() was null");

        Validate.notNull(handlerInput.getRequestEnvelope(),
                "handlerInput.getRequestEnvelope() was null");

        Validate.notNull(handlerInput.getRequestEnvelope().getSession(),
                "handlerInput.getRequestEnvelope().getSession() was null");

        Validate.notNull(handlerInput.getRequestEnvelope().getSession().getUser(),
                "handlerInput.getRequestEnvelope().getSession().getUser() was null");

        Validate.notBlank(handlerInput.getRequestEnvelope().getSession().getUser().getUserId(),
                "handlerInput.getRequestEnvelope().getSession().getUser().getUserId() was blank");
    }
}
