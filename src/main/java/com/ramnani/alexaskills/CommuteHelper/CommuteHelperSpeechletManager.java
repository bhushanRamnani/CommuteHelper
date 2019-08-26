package com.ramnani.alexaskills.CommuteHelper;


import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.services.ServiceException;
import com.amazon.ask.model.services.deviceAddress.Address;
import com.amazon.ask.model.services.deviceAddress.DeviceAddressServiceClient;
import com.amazon.ask.model.ui.AskForPermissionsConsentCard;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import com.ramnani.alexaskills.CommuteHelper.util.AlexaUtils;
import com.ramnani.alexaskills.CommuteHelper.util.Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;

import java.util.Optional;

public class CommuteHelperSpeechletManager {

    private static final Logger log = Logger.getLogger(CommuteHelperSpeechletManager.class);

    private static final String ALL_ADDRESS_PERMISSION = "read::alexa:device:all:address";

    protected final GoogleMapsService googleMaps;
    protected final TransitHelperDao userStore;

    public CommuteHelperSpeechletManager(final TransitHelperDao userStore,
                                         final GoogleMapsService googleMapsService) {
        Validate.notNull(googleMapsService);
        Validate.notNull(userStore);

        this.googleMaps = googleMapsService;
        this.userStore = userStore;
    }

    public ImmutablePair<Optional<String>, Optional<Response>> updateHomeAddressFromDeviceAddress(
            final HandlerInput handlerInput) {
        Validator.validateHandlerInput(handlerInput);

        log.info("Handling updateHomeAddressFromDeviceAddress: " + handlerInput.getRequestEnvelope());

        try {
            String addressContent = getDeviceAddress(handlerInput);
            String failedOutputSpeech = "Sorry. I could not get a valid device address. "
                    + "You can add or change your home address "
                    + "in the device settings using the alexa app on your phone. "
                    + "Once completed, please ask me again.";

            if (StringUtils.isBlank(addressContent)) {
                return ImmutablePair.of(Optional.empty(),
                        handlerInput.getResponseBuilder()
                                .withSpeech(failedOutputSpeech)
                                .withSimpleCard("Oops! Could not get device address", failedOutputSpeech)
                                .withShouldEndSession(true)
                                .build());
            }
            String gMapsAddress = googleMaps.getAddressOfPlace(addressContent);

            if (StringUtils.isBlank(gMapsAddress)) {
                return ImmutablePair.of(Optional.empty(), handlerInput.getResponseBuilder()
                        .withSpeech(failedOutputSpeech)
                        .withSimpleCard("Oops! Could not get device address", failedOutputSpeech)
                        .withShouldEndSession(true)
                        .build());
            }
            String userId = handlerInput.getRequestEnvelope().getSession().getUser().getUserId();
            String timezone = googleMaps.getTimezoneFromAddress(gMapsAddress);
            log.info("User timezone: " + timezone + ". UserId " +  userId);

            userStore.upsertUser(userId, gMapsAddress, null, timezone);

            return ImmutablePair.of(Optional.of(gMapsAddress), Optional.empty());

        } catch (ServiceException ex) {
            String error = "Could not retrieve user address. Status Code: " + ex.getStatusCode();

            if (ex.getStatusCode() == 403) {
                AskForPermissionsConsentCard card = AskForPermissionsConsentCard.builder()
                        .addPermissionsItem(ALL_ADDRESS_PERMISSION)
                        .build();

                return ImmutablePair.of(Optional.empty(), handlerInput.getResponseBuilder()
                        .withSpeech("In order to help you with transit information, I would need to access"
                                + " your home address. Is that OK?")
                        .withCard(card)
                        .withShouldEndSession(false)
                        .build());
            }
            log.error(error, ex);

            return ImmutablePair.of(Optional.empty(), handlerInput.getResponseBuilder()
                    .withSpeech(error)
                    .withShouldEndSession(true)
                    .build());
        }

    }

    public String getDeviceAddress(HandlerInput handlerInput) throws ServiceException {
        Validator.validateHandlerInput(handlerInput);

        DeviceAddressServiceClient deviceAddressServiceClient = handlerInput.getServiceClientFactory()
                .getDeviceAddressService();
        String deviceId = handlerInput.getRequestEnvelope()
                .getContext().getSystem().getDevice().getDeviceId();

        Address address = deviceAddressServiceClient.getFullAddress(deviceId);
        log.info("User Address obtained: " + address + ". User ID: "
                + handlerInput.getRequestEnvelope().getSession().getUser().getUserId());

        return AlexaUtils.generateAddressOutput(address);
    }
}
