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
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;

import java.util.Optional;

public class UpdateHomeAddressHandler implements IntentRequestHandler {

    private static final Logger log = Logger.getLogger(UpdateHomeAddressHandler.class);

    private UserSetupSpeechletManager userSetupSpeechletManager;

    public UpdateHomeAddressHandler(UserSetupSpeechletManager userSetupSpeechletManager) {
        Validate.notNull(userSetupSpeechletManager);

        this.userSetupSpeechletManager = userSetupSpeechletManager;
    }

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName("UpdateHomeAddress"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Validator.validateHandlerInput(input);
        Validator.validateIntentRequest(intentRequest);

        input.getAttributesManager().getSessionAttributes().clear();

        ImmutablePair<Optional<String>, Optional<Response>> updateDeviceAddressResponse =
                userSetupSpeechletManager.updateHomeAddressFromDeviceAddress(input);

        if (updateDeviceAddressResponse.getRight().isPresent()) {
            return updateDeviceAddressResponse.getRight();
        }
        String homeAddress = updateDeviceAddressResponse.getLeft().get();

        return input.getResponseBuilder()
                .withSpeech("Ok. Updated home address to " + homeAddress)
                .withSimpleCard("Home address  updated", homeAddress)
                .withShouldEndSession(false)
                .build();
    }
}
