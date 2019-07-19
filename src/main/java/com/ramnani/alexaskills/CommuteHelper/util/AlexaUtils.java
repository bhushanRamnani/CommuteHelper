package com.ramnani.alexaskills.CommuteHelper.util;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import org.apache.commons.lang3.Validate;

import java.util.Optional;

public final class AlexaUtils {

    public static final String ERROR_STRING = "Sorry. I'm having some issues " +
            "giving you an answer right now.";

    private AlexaUtils() {
    }

    public static final String getUserId(HandlerInput handlerInput) {
        Validator.validateHandlerInput(handlerInput);

        return handlerInput.getRequestEnvelope().getSession().getUser().getUserId();
    }

    public static Optional<Response> getInternalServerErrorResponse(HandlerInput handlerInput) {
        Validate.notNull(handlerInput);
        Validate.notNull(handlerInput.getResponseBuilder());

        return handlerInput.getResponseBuilder()
                .withSpeech(ERROR_STRING)
                .withSimpleCard("Oops!", ERROR_STRING)
                .withShouldEndSession(true)
                .build();
    }
}
