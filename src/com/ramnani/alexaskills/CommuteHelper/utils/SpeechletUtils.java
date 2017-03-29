package com.ramnani.alexaskills.CommuteHelper.utils;

import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import org.apache.commons.lang3.Validate;


public class SpeechletUtils {


    public static SpeechletResponse getNewAskResponse(String output, String title) {
        Validate.notNull(output);
        Validate.notNull(title);

        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(output);
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(output);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt, card);
    }

    public static SpeechletResponse getNewTellResponse(String output, String title) {
        Validate.notNull(output);
        Validate.notNull(title);

        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(output);
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(output);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);
        return SpeechletResponse.newTellResponse(outputSpeech, card);
    }

    public static Reprompt getReprompt(String repromptText) {
        Validate.notNull(repromptText);
        Reprompt reprompt = new Reprompt();
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(repromptText);
        reprompt.setOutputSpeech(speech);
        return reprompt;
    }
}
