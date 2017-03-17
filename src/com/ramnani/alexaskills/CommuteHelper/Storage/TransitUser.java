package com.ramnani.alexaskills.CommuteHelper.Storage;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.util.Map;


@DynamoDBTable(tableName="TransitUsers")
public class TransitUser {

    private String userId;
    private String homeAddress;
    private Map<String, String> destinations;

    @DynamoDBAttribute(attributeName="Destinations")
    public Map<String, String> getDestinations() {
        return destinations;
    }
    public void setDestinations(Map<String, String> destinations) {
        this.destinations = destinations;
    }

    @DynamoDBHashKey(attributeName="UserId")
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDBAttribute(attributeName="HomeAddress")
    public String getHomeAddress() {
        return homeAddress;
    }
    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    @Override
    public String toString() {
        return "UserId: " + userId + "\t" +
               "Home Address: " + homeAddress + "\t" +
               "Destinations: " + destinations;
    }
}
