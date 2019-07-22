/*
 * Copyright 2018-2019 Bhushan Ramnani (b.ramnani@gmail.com),
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
package com.ramnani.alexaskills.CommuteHelper.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import com.ramnani.alexaskills.CommuteHelper.util.Validator;
import org.apache.log4j.Logger;

import java.util.Optional;

public class HelpIntentHandler implements RequestHandler {

    private static final Logger log = Logger.getLogger(HelpIntentHandler.class);

    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(Predicates.intentName("AMAZON.HelpIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input) {
        Validator.validateHandlerInput(input);

        String speechText = "In order to get transit information from your home to work," +
                " you can ask me, \"when's the next bus to work\", or, \"when's the next transit" +
                " to work.\". After that, I can help you with more information, like arrival time," +
                " duration of travel, and directions.";

        return input.getResponseBuilder().withSpeech(speechText)
                .withSimpleCard("Usage", speechText)
                .withShouldEndSession(false)
                .withReprompt(speechText)
                .build();
    }
}
