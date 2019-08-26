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
package com.ramnani.alexaskills.CommuteHelper;

import com.amazon.ask.Skill;
import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import com.ramnani.alexaskills.CommuteHelper.handler.GetArrivalTimeHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.GetDirectionsHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.GetHomeAddressHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.GetNextTransitToLocationHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.GetTransitDurationHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.GetWorkAddressHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.HelpIntentHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.LaunchRequestHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.NextSuggestionHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.PreviousSuggestionHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.PutPostalAddressHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.RepeatSuggestionHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.StandardExceptionHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.StopOrCancelHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.UpdateHomeAddressHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.UpdateWorkAddressHandler;
import com.ramnani.alexaskills.CommuteHelper.handler.YesOrNoRequestHandler;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CommuteHelperStreamHandler extends SkillStreamHandler {

    private static ApplicationContext appContext;
    private static GoogleMapsService googleMapsService;
    private static TransitHelperDao transitHelperDao;
    private static UserSetupSpeechletManager userSetupSpeechletManager;
    private static TransitSpeechletManager transitSpeechletManager;

    private static Skill getSkill() {
        appContext =  new ClassPathXmlApplicationContext("application-config.xml");
        googleMapsService = (GoogleMapsService) appContext.getBean("googleMapsService");
        transitHelperDao = (TransitHelperDao) appContext.getBean("transitHelperDao");

        transitSpeechletManager = new TransitSpeechletManager(googleMapsService);
        userSetupSpeechletManager = new UserSetupSpeechletManager(transitHelperDao, googleMapsService);

        Validate.notNull(googleMapsService);
        Validate.notNull(transitHelperDao);

        return Skills.standard()
                .addRequestHandlers(
                        new LaunchRequestHandler(),
                        new HelpIntentHandler(),
                        new StopOrCancelHandler(),
                        new GetArrivalTimeHandler(transitSpeechletManager, userSetupSpeechletManager),
                        new GetDirectionsHandler(transitSpeechletManager),
                        new GetHomeAddressHandler(userSetupSpeechletManager),
                        new GetNextTransitToLocationHandler(transitSpeechletManager, userSetupSpeechletManager),
                        new GetTransitDurationHandler(transitSpeechletManager, userSetupSpeechletManager),
                        new GetWorkAddressHandler(userSetupSpeechletManager),
                        new NextSuggestionHandler(transitSpeechletManager),
                        new PreviousSuggestionHandler(transitSpeechletManager),
                        new RepeatSuggestionHandler(transitSpeechletManager),
                        new PutPostalAddressHandler(userSetupSpeechletManager),
                        new UpdateHomeAddressHandler(userSetupSpeechletManager),
                        new UpdateWorkAddressHandler(userSetupSpeechletManager),
                        new YesOrNoRequestHandler(transitSpeechletManager, userSetupSpeechletManager))
                .addExceptionHandler(new StandardExceptionHandler())
                .withSkillId("amzn1.ask.skill.87670333-a7fa-45d5-afbe-0a6030917bd8")
                .build();
    }

    public CommuteHelperStreamHandler() { super(getSkill()); }
}
