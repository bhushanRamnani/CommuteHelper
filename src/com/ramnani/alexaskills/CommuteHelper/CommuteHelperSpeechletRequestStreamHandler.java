package com.ramnani.alexaskills.CommuteHelper;

import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ramnanib on 12/4/16.
 */
public class CommuteHelperSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {

    private static final Set<String> supportedApplicationIds;
    private static final ApplicationContext appContext;
    private static final GoogleMapsService googleMapsService;
    private static final TransitHelperDao transitHelperDao;

    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        supportedApplicationIds = new HashSet<String>();
        supportedApplicationIds.add("amzn1.ask.skill.195ed8b2-5a91-403d-8aa5-f6c4837d066b");
        appContext =  new ClassPathXmlApplicationContext("application-config.xml");
        googleMapsService = (GoogleMapsService) appContext.getBean("googleMapsService");
        transitHelperDao = (TransitHelperDao) appContext.getBean("transitHelperDao");
    }

    public CommuteHelperSpeechletRequestStreamHandler() {
        super(new CommuteHelperSpeechlet(googleMapsService, transitHelperDao), supportedApplicationIds);
    }

    public CommuteHelperSpeechletRequestStreamHandler(Speechlet speechlet, Set<String> supportedApplicationIds) {
        super(speechlet, supportedApplicationIds);
    }
}
