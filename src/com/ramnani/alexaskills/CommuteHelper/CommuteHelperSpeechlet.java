package com.ramnani.alexaskills.CommuteHelper;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitUser;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;


public class CommuteHelperSpeechlet implements Speechlet {

    private static final Logger log = Logger.getLogger(CommuteHelperSpeechlet.class);

    private static final String ERROR_STRING = "Sorry. I'm having some issues " +
            "giving you an answer right now.";

    private TransitSpeechletManager transitSpeechletManager;

    private TransitHelperDao userStore;

    private UserSetupSpeechletManager userSetupSpeechletManager;

    public CommuteHelperSpeechlet(GoogleMapsService googleMapsService, TransitHelperDao transitHelperDao) {
        Validate.notNull(googleMapsService);

        transitSpeechletManager = new TransitSpeechletManager(googleMapsService);
        userStore = transitHelperDao;
        userSetupSpeechletManager = new UserSetupSpeechletManager(transitHelperDao, googleMapsService);
    }

    @Override
    public void onSessionStarted(SessionStartedRequest sessionStartedRequest,
                                 Session session) throws SpeechletException {
        log.info("onSessionStarted requestId=" + sessionStartedRequest.getRequestId() +
                        " sessionId=" + session.getSessionId());
    }

    @Override
    public SpeechletResponse onLaunch(LaunchRequest launchRequest,
                                      Session session) throws SpeechletException {
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(IntentRequest intentRequest, Session session) throws SpeechletException {
        log.info("onIntent requestId=" + intentRequest.getRequestId() +
                " sessionId=" + session.getSessionId());

        // check if the user exists in the database
        String user = session.getUser().getUserId();
        Intent intent = intentRequest.getIntent();
        String intentName = intent.getName();

        log.info("Intent Name: " + intentName);

        if ("AMAZON.CancelIntent".equals(intent.getName()) ||
                "AMAZON.StopIntent".equals(intent.getName())) {
            return handleExitIntentResponse();
        }
        TransitUser transitUser = userStore.getUser(user);

        if (transitUser == null) {
            log.info("User does not exist. Handling user setup. User: " + user);
            return userSetupSpeechletManager.handleUserSetup(session, intent);
        }
        log.info("User exists. User: " + user);

        try {
            if ("GetNextTransitToWork".equals(intentName)) {
                return transitSpeechletManager.handleNextTransitRequest(intent, session, transitUser);
            } else if ("GetArrivalTime".equals(intentName)) {
                return transitSpeechletManager.handleGetArrivalTimeRequest(intentRequest, session);
            } else if ("GetTotalTransitDuration".equals(intentName)) {
                return transitSpeechletManager.handleGetTotalTransitDurationRequest(session);
            } else if ("GetDirections".equals(intentName)) {
                return transitSpeechletManager.handleGetDirectionsRequest(session);
            } else if ("UpdateHomeAddress".equals(intentName)) {
                return userSetupSpeechletManager.handleUpdateHomeAddressRequest(session);
            } else if ("UpdateWorkAddress".equals(intentName)) {
                return userSetupSpeechletManager.handleUpdateWorkAddressRequest(session);
            } else if ("PutPostalAddress".equals(intentName)) {
                return userSetupSpeechletManager.handleUpdatePostalAddressRequest(session, intent);
            } else if ("YesIntent".equals(intentName) || "NoIntent".equals(intentName)) {
                return userSetupSpeechletManager.handleVerifyPostalAddressRequest(session, intent);
            } else if ("GetWorkAddress".equals(intentName)) {
                return userSetupSpeechletManager.handleGetWorkAddressRequest(user);
            } else if ("GetHomeAddress".equals(intentName)) {
                return userSetupSpeechletManager.handleGetHomeAddressRequest(user);
            } else if ("AMAZON.RepeatIntent".equals(intentName)) {
                return transitSpeechletManager.handleRepeatSuggestionRequest(session);
            } else if ("AMAZON.NextIntent".equals(intentName)) {
                return transitSpeechletManager.handleNextSuggestionRequest(session);
            } else if ("AMAZON.PreviousIntent".equals(intentName)) {
                return transitSpeechletManager.handlePreviousSuggestionRequest(session);
            } else {
                throw new IllegalArgumentException("Unrecognized intent: " + intent.getName());
            }
        } catch (Exception ex) {
            log.error("Internal Server error handling the intent.", ex);
            return transitSpeechletManager.getTryAgainResponse(ERROR_STRING);
        }
    }

    @Override
    public void onSessionEnded(SessionEndedRequest sessionEndedRequest,
                               Session session) throws SpeechletException {
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse() {
        String speechText = "Hi! I'm Transit Companion. " +
                "I'll be glad to help you with transit information." +
                "For example, ask me when's your next bus.";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Transit Companion");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    private SpeechletResponse handleExitIntentResponse() {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        String byeText = "Bye. Have a nice ride.";
        speech.setText(byeText);
        SimpleCard card = new SimpleCard();
        card.setContent(byeText);
        return SpeechletResponse.newTellResponse(speech, card);
    }
}
