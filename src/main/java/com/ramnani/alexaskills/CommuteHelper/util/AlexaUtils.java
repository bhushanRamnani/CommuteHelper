package com.ramnani.alexaskills.CommuteHelper.util;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.deviceAddress.Address;
import org.apache.commons.lang3.StringUtils;
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

    public static String generateAddressOutput(Address address, boolean asSpeech) {
        StringBuilder addressSpeech = new StringBuilder();
        appendToString(addressSpeech, address.getAddressLine1(), false);
        appendToString(addressSpeech, address.getAddressLine2(), false);
        appendToString(addressSpeech, address.getAddressLine3(), false);
        appendToString(addressSpeech, address.getCity(), false);

        if (!StringUtils.isBlank(address.getPostalCode())) {
            String postalCode = address.getPostalCode();

            if (asSpeech) {
                postalCode = "<say-as interpret-as=\"cardinal\">" + address.getPostalCode() + "</say-as>";
            }
            appendToString(addressSpeech, postalCode, false);
        }
        appendToString(addressSpeech, address.getCountryCode(), true);
        return addressSpeech.toString();
    }

    private static void appendToString(StringBuilder stringBuilder, String addressLine, boolean lastLine) {
        if (!StringUtils.isBlank(addressLine)) {
            stringBuilder.append(addressLine);

            if (!lastLine) {
                stringBuilder.append(", ");
            }
        }
    }
}
