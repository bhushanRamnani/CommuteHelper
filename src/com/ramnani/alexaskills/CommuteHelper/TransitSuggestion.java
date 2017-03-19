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

import com.google.maps.model.Duration;
import org.apache.commons.lang3.Validate;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;


public class TransitSuggestion {

    /**
     * What type of transit vehicle is this. eg. Bus, monorail, etc.
     */
    private String transitType;

    /**
     * At what time are you going to start walking
     */
    private DateTime walkingStartTime;

    /**
     * At what time is this transit vehicle going to start.
     */
    private DateTime transitStartTime;

    /**
     * At what time will I reach my destination
     */
    private DateTime arrivalTime;

    /**
     * Total amount of time it will take to reach the
     * final destication
     */
    private Duration totalDuration;

    /**
     * Duration to walk
     */
    private Duration walkingDuration;

    /**
     * Duration to transit
     */
    private Duration transitDuration;

    /**
     * High level walking instruction. eg. Walk to 10th Ave E & E Roanoke St
     */
    private String walkingInstruction;

    /**
     * High level transit instruction. eg. Bus towards Downtown Seattle Broadway
     */
    private String transitInstruction;

    /**
     * The identification of this transit vehicle. Eg. 49 (bus number 49).
     */
    private String transitId;

    /**
     * If there is a switch to a different transit.
     * eg. This will be 0 if there's only one transit switch.
     */
    private int numOfSwitches;

    public TransitSuggestion() {
    }

    public TransitSuggestion(String transitType,
                             DateTime walkingStartTime,
                             DateTime transitStartTime,
                             DateTime arrivalTime, Duration totalDuration, Duration walkingDuration,
                             Duration transitDuration,
                             String walkingInstruction,
                             String transitInstruction,
                             String transitId,
                             int numOfSwitches) {
        Validate.notNull(transitStartTime);
        Validate.notNull(transitDuration);
        Validate.notNull(arrivalTime);
        Validate.notNull(totalDuration);

        this.setArrivalTime(arrivalTime);
        this.setTotalDuration(totalDuration);
        this.setTransitType(transitType);
        this.setWalkingStartTime(walkingStartTime);
        this.setTransitStartTime(transitStartTime);
        this.setWalkingDuration(walkingDuration);
        this.setTransitDuration(transitDuration);
        this.setWalkingInstruction(walkingInstruction);
        this.setTransitInstruction(transitInstruction);
        this.setTransitId(transitId);
        this.setNumOfSwitches(numOfSwitches);
    }

    @JsonIgnore
    public int getTimeToTransitArrivalInMinutes() {
        return Minutes.minutesBetween(DateTime.now(), getTransitStartTime())
                      .getMinutes();
    }

    @JsonIgnore
    public int getLeavingTimeInSeconds() {
        DateTime leavingStartTime = getWalkingStartTime() != null
                ? getWalkingStartTime() : getTransitStartTime();

        return Seconds.secondsBetween(DateTime.now(), leavingStartTime)
                      .getSeconds();

    }

    @Override
    @JsonIgnore
    public String toString() {
        return "Transit Type : " + getTransitType() + "\n" +
               "Walking Start Time: " + getWalkingStartTime() + "\n" +
               "Walking Duration: " + getWalkingDuration().humanReadable + "\n" +
               "Transit Duration: " + getTransitDuration().humanReadable + "\n" +
               "Walking Instruction: " + getWalkingInstruction() + "\n" +
               "Transit Instruction: " + getTransitInstruction() + "\n" +
               "Transit ID: " + getTransitId() + "\n" +
               "Number of Switches: " + getNumOfSwitches() +"\n" +
               "Total Duration: " + getTotalDuration().humanReadable + "\n" +
               "Arrival Time: " + getArrivalTime() + "\n";
    }

    public String getTransitType() {
        return transitType;
    }

    public void setTransitType(String transitType) {
        this.transitType = transitType;
    }

    public DateTime getWalkingStartTime() {
        return walkingStartTime;
    }

    public void setWalkingStartTime(DateTime walkingStartTime) {
        this.walkingStartTime = walkingStartTime;
    }

    public DateTime getTransitStartTime() {
        return transitStartTime;
    }

    public void setTransitStartTime(DateTime transitStartTime) {
        this.transitStartTime = transitStartTime;
    }

    public DateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(DateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Duration getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Duration totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Duration getWalkingDuration() {
        return walkingDuration;
    }

    public void setWalkingDuration(Duration walkingDuration) {
        this.walkingDuration = walkingDuration;
    }

    public Duration getTransitDuration() {
        return transitDuration;
    }

    public void setTransitDuration(Duration transitDuration) {
        this.transitDuration = transitDuration;
    }

    public String getWalkingInstruction() {
        return walkingInstruction;
    }

    public void setWalkingInstruction(String walkingInstruction) {
        this.walkingInstruction = walkingInstruction;
    }

    public String getTransitInstruction() {
        return transitInstruction;
    }

    public void setTransitInstruction(String transitInstruction) {
        this.transitInstruction = transitInstruction;
    }

    public String getTransitId() {
        return transitId;
    }

    public void setTransitId(String transitId) {
        this.transitId = transitId;
    }

    public int getNumOfSwitches() {
        return numOfSwitches;
    }

    public void setNumOfSwitches(int numOfSwitches) {
        this.numOfSwitches = numOfSwitches;
    }
}
