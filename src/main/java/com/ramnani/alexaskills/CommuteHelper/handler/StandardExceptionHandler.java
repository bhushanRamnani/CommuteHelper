package com.ramnani.alexaskills.CommuteHelper.handler;

import com.amazon.ask.dispatcher.exception.ExceptionHandler;
import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import org.apache.log4j.Logger;

import java.util.Optional;

public class StandardExceptionHandler implements ExceptionHandler {

    private static final Logger log = Logger.getLogger(StandardExceptionHandler.class);

    private static final String ERROR_STRING = "Sorry. I'm having some issues " +
            "giving you an answer right now.";

    @Override
    public boolean canHandle(HandlerInput handlerInput, Throwable throwable) {
        return true;
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput, Throwable throwable) {
        log.error("Failed to process request: " + handlerInput.getRequestEnvelope(), throwable);

        return handlerInput.getResponseBuilder()
                .withSpeech(ERROR_STRING)
                .withSimpleCard("Oops!", ERROR_STRING)
                .withShouldEndSession(true)
                .build();
    }
}
