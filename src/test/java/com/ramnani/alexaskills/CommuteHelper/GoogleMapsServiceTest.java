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

import com.google.maps.model.PlacesSearchResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by ramnanib on 12/4/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-config.xml")
public class GoogleMapsServiceTest {

    @Autowired
    private GoogleMapsService service;

    @Test
    public void getNextBusToWork_sanity() {
        List<TransitSuggestion> suggestions = service.getNextTransitToDestination(
                "bus", "422 S Zarzamora St, San Antonio, TX 78207",
                "1300 Delgado St, San Antonio, TX 78207");
        assertNotNull(suggestions);
        int leavingStartTime = suggestions.get(0).getLeavingTimeInSeconds();
        assertTrue(leavingStartTime >= 0);
        assertTrue(suggestions.size() > 0);
    }

    @Test
    public void getNearbyAddress_sanity() {
        PlacesSearchResult placesSearchResult = service.getNearbyPlaceAddress("the museum",
                "Mumbai");

        assertNotNull(placesSearchResult);
        assertTrue(StringUtils.isNotBlank(placesSearchResult.formattedAddress));
        assertTrue(StringUtils.isNotBlank(placesSearchResult.name));
        System.out.println(placesSearchResult.formattedAddress);
        System.out.println(placesSearchResult.name);
    }

    @Test
    public void getAddressOfPlace_sanity() {
        String address = service.getAddressOfPlace("5069 Bunker St NW Tracyton");
        assertTrue(!StringUtils.isBlank(address));
        System.out.println("getNextTransitToPlace_sanity. Address: " + address);
    }

    @Test
    public void getTimezoneFromAddress_sanity() {
        String timezone = service.getTimezoneFromAddress("5069 Bunker St NW Tracyton");
        assertNotNull(timezone);
        Assert.assertEquals("America/Los_Angeles", timezone);
    }
}
