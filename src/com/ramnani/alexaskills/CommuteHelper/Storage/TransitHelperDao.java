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
package com.ramnani.alexaskills.CommuteHelper.Storage;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Access Object layer for The TransitUsers DynamoDB table
 */
public class TransitHelperDao {

    AmazonDynamoDBClient dynamoDBClient;
    DynamoDBMapper mapper;

    public TransitHelperDao() {
        dynamoDBClient = new AmazonDynamoDBClient();
        mapper = new DynamoDBMapper(dynamoDBClient);
    }

    /**
     * Return a user from TransitUsers table queried on userId which is the partition key
     */
    public TransitUser getUser(String userId) {
        Validate.notNull(userId);
        Validate.notEmpty(userId);
        TransitUser user = mapper.load(TransitUser.class, userId);
        return user;
    }

    /**
     * Add a new user into the TransitUsers table. Adds the userId and the
     * homeAddress. If the user with the specified userId already exists, the user
     * information will be updated
     *
     * @return The TransitUser object that's inserted into the table
     */
    public TransitUser upsertUser(String userId, String homeAddress) {
        Validate.notNull(userId);
        Validate.notNull(homeAddress);

        TransitUser user = new TransitUser();
        user.setUserId(userId);
        user.setHomeAddress(homeAddress);
        mapper.save(user);
        return user;
    }

    /**
     * Add a new user into the TransitUsers table. Adds the userId, the
     * homeAddress and the destinations. The 'destinations' is a map of
     * DestinationName -> DestinationAddress
     *
     * If the user with the specified userId already exists, the user
     * information will be updated
     *
     * @return The TransitUser object that's inserted into the table
     */
    public TransitUser upsertUser(String userId, String homeAddress,
                               Map<String, String> destinations) {
        Validate.notNull(userId);
        Validate.notNull(homeAddress);
        Validate.notNull(destinations);

        TransitUser user = new TransitUser();
        user.setUserId(userId);
        user.setHomeAddress(homeAddress);
        user.setDestinations(destinations);
        mapper.save(user);
        return user;
    }

    /**
     * Update Home address of an existing user
     */
    public TransitUser updateHomeAddress(String userId, String homeAddress) {
        Validate.notNull(userId);
        Validate.notNull(homeAddress);

        TransitUser user = getUser(userId);

        if (user == null) {
            throw new IllegalArgumentException("User does not exist: " + userId);
        }
        user.setHomeAddress(homeAddress);
        mapper.save(user);
        return user;
    }

    /**
     * Adds a destination for the user. If the destinationName already exists
     * for the specified user, the address will be updated. If the user with
     * the specified userID does not exist, then this method will throw an
     * IllegalArgument Exception.
     */
    public void addOrUpdateDestination(String userId, String name,
                                       String destinationAddress) {
        Validate.notNull(userId);
        Validate.notNull(name);
        Validate.notNull(destinationAddress);

        TransitUser user = getUser(userId);

        if (user == null) {
            throw new IllegalArgumentException("User does not exist: " + userId);
        }
        Map<String, String> destinations = user.getDestinations();

        if (destinations == null) {
            destinations = new HashMap<>();
            user.setDestinations(destinations);
        }
        destinations.put(name, destinationAddress);
        mapper.save(user);
    }

    /**
     * Deletes a user with the specified userId from the TransitUsers table
     */
    public void deleteUser(String userId) {
        Validate.notNull(userId);
        TransitUser deleteUser = new TransitUser();
        deleteUser.setUserId(userId);
        mapper.delete(deleteUser);
    }

    /**
     * Checks if the user with the specified userId exists in the TransitUsers table
     */
    public boolean containsUser(String userId) {
        TransitUser user = getUser(userId);
        return user != null;
    }
}
