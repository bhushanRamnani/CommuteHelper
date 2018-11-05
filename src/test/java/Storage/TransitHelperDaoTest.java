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
package Storage;

import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitUser;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit Tests for TransitHelperDao
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-config.xml")
public class TransitHelperDaoTest {

    private String myUserId = "userId";
    private String timezone = "America/Los_Angeles";

    @Autowired
    private TransitHelperDao transitHelperDao;

    @Test
    public void addNewUser_NoDestinations() {
        String userId1 = "User1";
        String address1 = "Address1";
        TransitUser user = transitHelperDao.upsertUser(userId1, address1);
        Assert.assertEquals(userId1, user.getUserId());
        Assert.assertEquals(address1, user.getHomeAddress());
        TransitUser userFromDb = transitHelperDao.getUser(userId1);
        Assert.assertEquals(userId1, userFromDb.getUserId());
        Assert.assertEquals(address1, userFromDb.getHomeAddress());
        transitHelperDao.deleteUser(userId1);
    }

    @Test
    public void addNewUser_withTwoDestinations() {
        String userId1 = "amzn1.ask.account.AHDOT44PW7ZLQ2ZDHRORE5G24WXP2CVUMNNRDEFOSWTCCCDQQEE4Q2Z4M4IKUU2EW4RMPVT57GVRRDIGAYEYUBTDJAVYHCGHVYC7QJFRVLL36QOYNR3IIAWP4PTQIYT7ZD3WARDF337F2MNYX5GQ";
        String address1 = "2561 Toyer Ave W. Seattle, WA - 98123";
        Map<String, String> destinations = new HashMap<>();
        destinations.put("Sam's place", "1919 55th Ave, Seattle, WA - 98223");
        destinations.put("work", "1918 8th Avenue, Seattle, Washington");
        TransitUser user = transitHelperDao.upsertUser(userId1, address1, destinations, timezone);
        Assert.assertEquals(userId1, user.getUserId());
        Assert.assertEquals(address1, user.getHomeAddress());
        Assert.assertEquals(destinations, user.getDestinations());
        TransitUser userFromDb = transitHelperDao.getUser(userId1);
        Assert.assertEquals(userId1, userFromDb.getUserId());
        Assert.assertEquals(address1, userFromDb.getHomeAddress());
        Assert.assertEquals(destinations, userFromDb.getDestinations());
        transitHelperDao.deleteUser(userId1);
    }

    @Test
    public void updateUserAddress() {
        String userId1 = "User1";
        String address1 = "Address1";
        String address2 = "Address2";
        TransitUser user = transitHelperDao.upsertUser(userId1, address1);
        Assert.assertEquals(userId1, user.getUserId());
        Assert.assertEquals(address1, user.getHomeAddress());
        TransitUser userFromDb = transitHelperDao.getUser(userId1);
        Assert.assertEquals(userId1, userFromDb.getUserId());
        Assert.assertEquals(address1, userFromDb.getHomeAddress());
        TransitUser updatedUser = transitHelperDao.upsertUser(userId1, address2);
        Assert.assertEquals(userId1, updatedUser.getUserId());
        Assert.assertEquals(address2, updatedUser.getHomeAddress());
        TransitUser updatedUserFromDb = transitHelperDao.getUser(userId1);
        Assert.assertEquals(userId1, updatedUserFromDb.getUserId());
        Assert.assertEquals(address2, updatedUserFromDb.getHomeAddress());
        transitHelperDao.deleteUser(userId1);
    }

    @Test
    public void addDestinations_withNoDestinations() {
        String userId = "User3";
        String address = "Address3";
        String destinationName1 = "Sam's place";
        String destinationName2 = "Work";
        String destinationAddress1 = "1919 55th Ave, Seattle, WA - 98223";
        String destinationAddress2 =  "3211 43rd Ave, Seattle, WA - 94445";
        Map<String, String> destinations = new HashMap<>();
        destinations.put(destinationName1, destinationAddress1);
        destinations.put(destinationName2, destinationAddress2);
        TransitUser user = transitHelperDao.upsertUser(userId, address);
        Assert.assertEquals(userId, user.getUserId());
        Assert.assertEquals(address, user.getHomeAddress());
        TransitUser userFromDb = transitHelperDao.getUser(userId);
        Assert.assertEquals(userId, userFromDb.getUserId());
        Assert.assertEquals(address, userFromDb.getHomeAddress());
        transitHelperDao.addOrUpdateDestination(userId, destinationName1, destinationAddress1);
        transitHelperDao.addOrUpdateDestination(userId, destinationName2, destinationAddress2);
        userFromDb = transitHelperDao.getUser(userId);
        Assert.assertEquals(destinations, userFromDb.getDestinations());
        Assert.assertEquals(userId, userFromDb.getUserId());
        Assert.assertEquals(address, userFromDb.getHomeAddress());
        transitHelperDao.deleteUser(userId);
    }

    @Test
    public void updateUserDestinations() {
        String userId = "User4";
        String address = "Address4";
        String destinationName1 = "Sam's place";
        String destinationName2 = "Work";
        String destinationAddress1 = "1919 55th Ave, Seattle, WA - 98223";
        String destinationAddress2 =  "3211 43rd Ave, Seattle, WA - 94445";
        Map<String, String> destinations = new HashMap<>();
        destinations.put(destinationName1, destinationAddress1);
        destinations.put(destinationName2, destinationAddress2);
        TransitUser user = transitHelperDao.upsertUser(userId, address, destinations, timezone);
        Assert.assertEquals(userId, user.getUserId());
        Assert.assertEquals(address, user.getHomeAddress());
        Assert.assertEquals(destinations, user.getDestinations());
        TransitUser userFromDb = transitHelperDao.getUser(userId);
        Assert.assertEquals(userId, userFromDb.getUserId());
        Assert.assertEquals(address, userFromDb.getHomeAddress());
        Assert.assertEquals(destinations, userFromDb.getDestinations());

        Map<String, String> destinations2 = new HashMap<>();
        destinations2.put("Tom's place", "11000 55th Ave, San Francisco, CA - 98223");
        destinations2.put("Gym", "32312 43rd Ave, San Francisco, CA - 94445");
        TransitUser updatedUser = transitHelperDao.upsertUser(userId, address, destinations2, timezone);
        Assert.assertEquals(userId, updatedUser.getUserId());
        Assert.assertEquals(address, updatedUser.getHomeAddress(), updatedUser.getHomeAddress());
        Assert.assertEquals(destinations2, updatedUser.getDestinations());
        userFromDb = transitHelperDao.getUser(userId);
        Assert.assertEquals(userId, userFromDb.getUserId());
        Assert.assertEquals(address, userFromDb.getHomeAddress());
        Assert.assertEquals(destinations2, userFromDb.getDestinations());
        transitHelperDao.deleteUser(userId);
    }

    @Test
    public void addDestinations_withExistingDestinations() {
        String userId = "User4";
        String address = "Address4";
        String destinationName1 = "Sam's place";
        String destinationName2 = "Work";
        String destinationAddress1 = "1919 55th Ave, Seattle, WA - 98223";
        String destinationAddress2 =  "3211 43rd Ave, Seattle, WA - 94445";
        Map<String, String> destinations = new HashMap<>();
        destinations.put(destinationName1, destinationAddress1);
        TransitUser user = transitHelperDao.upsertUser(userId, address, destinations, timezone);
        Assert.assertEquals(userId, user.getUserId());
        Assert.assertEquals(address, user.getHomeAddress());
        Assert.assertEquals(destinations, user.getDestinations());
        TransitUser userFromDb = transitHelperDao.getUser(userId);
        Assert.assertEquals(userId, userFromDb.getUserId());
        Assert.assertEquals(address, userFromDb.getHomeAddress());
        Assert.assertEquals(destinations, userFromDb.getDestinations());
        transitHelperDao.addOrUpdateDestination(userId, destinationName2, destinationAddress2);
        destinations.put(destinationName2, destinationAddress2);
        userFromDb = transitHelperDao.getUser(userId);
        Assert.assertEquals(destinations, userFromDb.getDestinations());
        Assert.assertEquals(userId, userFromDb.getUserId());
        Assert.assertEquals(address, userFromDb.getHomeAddress());
        transitHelperDao.deleteUser(userId);
    }

    @Test
    public void updateDestination() {
        String userId = "User4";
        String address = "Address4";
        Map<String, String> destinations = new HashMap<>();
        destinations.put("Sam's place", "1919 55th Ave, Seattle, WA - 98223");
        destinations.put("Work", "3211 43rd Ave, Seattle, WA - 94445");
        TransitUser user = transitHelperDao.upsertUser(userId, address, destinations, timezone);
        Assert.assertEquals(userId, user.getUserId());
        Assert.assertEquals(address, user.getHomeAddress());
        Assert.assertEquals(destinations, user.getDestinations());
        TransitUser userFromDb = transitHelperDao.getUser(userId);
        Assert.assertEquals(userId, userFromDb.getUserId());
        Assert.assertEquals(address, userFromDb.getHomeAddress());
        Assert.assertEquals(destinations, userFromDb.getDestinations());

        String updatedWorkDestination = "42323 Houston Ave, Wonderland, India";
        transitHelperDao.addOrUpdateDestination(userId, "Work", updatedWorkDestination);
        destinations.put("Work", updatedWorkDestination);
        user = transitHelperDao.upsertUser(userId, address, destinations, timezone);
        Assert.assertEquals(destinations, user.getDestinations());
        userFromDb = transitHelperDao.getUser(userId);
        Assert.assertEquals(destinations, userFromDb.getDestinations());
        transitHelperDao.deleteUser(userId);
    }
}
