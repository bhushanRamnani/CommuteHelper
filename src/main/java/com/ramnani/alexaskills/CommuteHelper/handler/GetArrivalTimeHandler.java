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

import java.util.Optional;

public class GetArrivalTimeHandler implements IntentRequestHandler {

    private static final Logger log = Logger.getLogger(GetArrivalTimeHandler.class);

    private TransitSpeechletManager transitSpeechletManager;
    private UserSetupSpeechletManager userSetupSpeechletManager;

    public GetArrivalTimeHandler(TransitSpeechletManager transitSpeechletManager,
                                 UserSetupSpeechletManager userSetupSpeechletManager) {
        this.transitSpeechletManager = transitSpeechletManager;
        this.userSetupSpeechletManager = userSetupSpeechletManager;
    }

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName("GetArrivalTime"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Validator.validateHandlerInput(input);
        Validator.validateIntentRequest(intentRequest);

        Optional<TransitUser> transitUser = userSetupSpeechletManager.getTransitUser(input);

        if (!transitUser.isPresent()) {
            log.info("Transit user does not exist. Going through user setup: " + AlexaUtils.getUserId(input));
            return userSetupSpeechletManager.handleUserSetup(input);
        }
        return transitSpeechletManager.handleGetArrivalTimeRequest(intentRequest, input, transitUser.get());
    }
}
