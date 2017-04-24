/*
 * Copyright 2016-2017 Bhushan Ramnani (b.ramnani@gmail.com),
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
package com.ramnani.alexaskills.CommuteHelper;

import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashSet;
import java.util.Set;


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
        supportedApplicationIds = new HashSet<>();
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
