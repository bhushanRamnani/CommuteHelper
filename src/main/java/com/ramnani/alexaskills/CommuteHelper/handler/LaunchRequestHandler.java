package com.ramnani.alexaskills.CommuteHelper.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;

import java.util.Optional;

public class LaunchRequestHandler implements RequestHandler {
    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.requestType(LaunchRequest.class));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        String speechText = "Hi! I'm Transit Helper Gamma Version 2. " +
                "I'll be glad to help you with transit information from" +
                " home to work. For example, you can ask me, " +
                "\"when's the next bus to work\".";
        return input.getResponseBuilder()
                .withSpeech(speechText)
                .withSimpleCard("Transit Helper", speechText)
                .withReprompt(speechText)
                .build();
    }
}
