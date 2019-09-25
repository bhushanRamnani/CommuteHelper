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
import com.ramnani.alexaskills.CommuteHelper.util.Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Optional;

import static com.ramnani.alexaskills.CommuteHelper.TransitSpeechletManager.SUGGESTION_TRANSIT_TYPE_ATTRIBUTE;
import static com.ramnani.alexaskills.CommuteHelper.UserSetupSpeechletManager.DESTINATION_ATTRIBUTE;

public class PutLocationNameHandler implements IntentRequestHandler {

    private static final Logger log = Logger.getLogger(PutLocationNameHandler.class);

    private static final String SLOT_LOCATION = "location";

    private UserSetupSpeechletManager userSetupSpeechletManager;
    private TransitSpeechletManager transitSpeechletManager;

    public PutLocationNameHandler(TransitSpeechletManager transitSpeechletManager,
                                  UserSetupSpeechletManager userSetupSpeechletManager) {
        Validate.notNull(userSetupSpeechletManager, "userSetupSpeechletManager cannot be null");
        Validate.notNull(transitSpeechletManager, "transitSpeechletManager cannot be null");

        this.userSetupSpeechletManager = userSetupSpeechletManager;
        this.transitSpeechletManager = transitSpeechletManager;
    }

    @Override
    public boolean canHandle(HandlerInput input, IntentRequest intentRequest) {
        return input.matches(Predicates.intentName("PutLocationName"));
    }

    @Override
    public Optional<Response> handle(HandlerInput input, IntentRequest intentRequest) {
        Validator.validateHandlerInput(input);
        Validator.validateIntentRequest(intentRequest);

        Optional<TransitUser> transitUser = userSetupSpeechletManager.getTransitUser(input);

        Map<String, Object> session = input.getAttributesManager().getSessionAttributes();

        if (!transitUser.isPresent()) {
            log.info("Transit user is not preset. Handling user setup first. Request: " + input.getRequest().getRequestId());
            return userSetupSpeechletManager.handleUserSetup(input);
        }

        if (session.containsKey(DESTINATION_ATTRIBUTE)
                && StringUtils.isNotBlank((String) session.get(DESTINATION_ATTRIBUTE))) {
            log.info("Inside PutLocationName. Location setup is on-going. Request: " + input.getRequest().getRequestId());
            return userSetupSpeechletManager.handlePutLocationName(input, intentRequest.getIntent());
        }
        log.info("Inside PutLocationName. Transit user exists and has requested for a transit suggestion. "
                + "User: " + transitUser.get().getUserId() + ". Request: " + input.getRequest().getRequestId());

        String location = intentRequest.getIntent().getSlots().get(SLOT_LOCATION).getValue();
        String transitType = (String) session.get(SUGGESTION_TRANSIT_TYPE_ATTRIBUTE);
        return transitSpeechletManager.handleNextTransitRequest(Optional.ofNullable(location),
                Optional.ofNullable(transitType), transitUser.get(), input, intentRequest.getIntent());
    }
}
