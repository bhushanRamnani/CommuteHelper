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

import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.TextSearchRequest;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.Duration;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import com.google.maps.model.TravelMode;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class GoogleMapsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleMapsService.class);
    private GeoApiContext geoApiContext;
    private static final String[] GENERIC_TRANSIT_TYPES = { "commute", "transit" };

    public GoogleMapsService(String apiKey) {
        geoApiContext = new GeoApiContext();
        geoApiContext.setApiKey(apiKey);
    }

    public String getAddressOfPlace(String placeName) {
        if (placeName == null || placeName.isEmpty()) {
            log.warn("placeName is null or empty.");
            return null;
        }
        TextSearchRequest request = PlacesApi.textSearchQuery(geoApiContext, placeName);
        PlacesSearchResponse response = request.awaitIgnoreError();

        if (response == null) {
            log.warn("Response from maps service returned null for place: " + placeName);
            return null;
        }

        if (response.results.length == 0) {
            log.warn("No results returned in response for place: " + placeName
                    + ". Response: " + response.toString());
            return null;
        }
        PlacesSearchResult place = response.results[0];
        log.info("Place returned from maps service: " + place.toString());

        if (place == null) {
            log.warn("No place returned as part of response result. Place: " + placeName
                    + ". Response: " + response.toString());
        }
        return place.formattedAddress;
    }

    public List<TransitSuggestion> getNextTransitToDestination(String transitType,
                                                               String homeAddress,
                                                               String destinationAddress) {
        Validate.notNull(transitType);
        Validate.notEmpty(transitType);

        DirectionsApiRequest request = DirectionsApi.getDirections(geoApiContext, homeAddress, destinationAddress);
        request.mode(TravelMode.TRANSIT);
        request.alternatives(true);
        request.departureTime(Instant.now());
        DirectionsRoute[] routes = request.awaitIgnoreError();

        if (routes == null || routes.length == 0) {
            return null;
        }
        Stream<DirectionsRoute> routeStream;

        if (!Arrays.asList(GENERIC_TRANSIT_TYPES).contains(transitType)) {
            routeStream = Stream.of(routes).filter(r -> isRouteATransitType(r, transitType));
        } else {
            routeStream = Stream.of(routes);
        }
        return routeStream.map(this::routeToSuggestionMap)
                          .filter(s -> s!=null)
                          .collect(Collectors.toList());
    }

    private TransitSuggestion routeToSuggestionMap(DirectionsRoute route) {
        DirectionsLeg[] legs = route.legs;

        if (legs == null || legs.length == 0) {
            return null;
        }
        DirectionsLeg leg = legs[0];
        DirectionsStep[] steps = leg.steps;

        if (steps == null || steps.length == 0) {
            return null;
        }
        DirectionsStep transitStep;
        Duration walkingDuration = null;
        DateTime walkingStartTime = null;
        String walkingInstruction = null;
        int transitStepIndex = 0;

        if (steps.length >= 2 && steps[0].travelMode == TravelMode.WALKING) {
            DirectionsStep walkingStep = steps[0];
            walkingDuration = walkingStep.duration;
            walkingStartTime = leg.departureTime;
            walkingInstruction = walkingStep.htmlInstructions;
            transitStep = steps[1];
            transitStepIndex = 1;
        } else {
            transitStep = steps[0];
        }

        try {
            Validate.isTrue(transitStep.travelMode == TravelMode.TRANSIT);
            Validate.notNull(transitStep.transitDetails);
            Validate.notNull(transitStep.transitDetails.line);
            Validate.notNull(transitStep.transitDetails.line.vehicle);

            DateTime transitStartTime = transitStep.transitDetails.departureTime;
            Validate.notNull(transitStartTime);

            Duration transitDuration = transitStep.duration;
            Validate.notNull(transitDuration);

            Duration totalDuration = leg.duration;
            Validate.notNull(totalDuration);

            DateTime arrivalTime = leg.arrivalTime;
            Validate.notNull(arrivalTime);

            String transitType = transitStep.transitDetails.line.vehicle.name;
            String transitInstruction = transitStep.htmlInstructions;
            String transitId = transitStep.transitDetails.line.shortName;
            int numSwitches = 0;

            for (int i=transitStepIndex+1; i<steps.length; i++) {
                if (steps[i].travelMode == TravelMode.TRANSIT) {
                    numSwitches++;
                }
            }
            return new TransitSuggestion(
                    transitType,
                    walkingStartTime,
                    transitStartTime,
                    arrivalTime,
                    totalDuration,
                    walkingDuration,
                    transitDuration,
                    walkingInstruction,
                    transitInstruction,
                    transitId,
                    numSwitches);
        } catch (IllegalArgumentException ex) {
            log.error("Error in parsing a DirectionsRoute response returned by maps API: ", ex);
        }
        return null;
    }

    private boolean isRouteATransitType(DirectionsRoute route, String transitType) {
        DirectionsLeg[] legs = route.legs;

        if (legs == null) {
            return false;
        }

        for (DirectionsLeg leg:legs) {
            DirectionsStep[] steps = leg.steps;

            if (steps == null) {
                return false;
            }

            for (DirectionsStep step:steps) {
                if (step.transitDetails != null
                 && step.transitDetails.line != null
                 && step.transitDetails.line.vehicle != null
                 && step.transitDetails.line.vehicle.name != null) {
                    String vehicleType = step.transitDetails.line.vehicle.name;

                    if (transitType.contains(vehicleType.toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
