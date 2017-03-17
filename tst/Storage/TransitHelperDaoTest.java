package Storage;

import com.ramnani.alexaskills.CommuteHelper.Storage.TransitHelperDao;
import com.ramnani.alexaskills.CommuteHelper.Storage.TransitUser;
import org.junit.Assert;
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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-config.xml")
public class TransitHelperDaoTest {

    private String myUserId = "amzn1.ask.account.AHHQSBLXNBOD2UICYL6N2DHPXTRXS7MGM7CQHS7GHQ44OPKK2PIXTSIJIQD42S6ILT5ZZ3TT22QPLF4JWSRC637JHUSRRXG7ELVZLFUSCJ2GIH2ADPZSLOOKQLJNQQ7DW3BW3KILKQCLYRLV7NRNHKDI3EHI5PKG546KQKHIMYHNU53MTQSVDQ5HWPUKGNR2SOKARINIYCM375Q";

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
        String userId1 = "User2";
        String address1 = "2400 Boyer Ave E, Seattle, WA 98112";
        Map<String, String> destinations = new HashMap<>();
        destinations.put("Sam's place", "1919 55th Ave, Seattle, WA - 98223");
        destinations.put("work", "1918 8th Ave, Seattle, WA 98101");
        TransitUser user = transitHelperDao.upsertUser(userId1, address1, destinations);
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
        TransitUser user = transitHelperDao.upsertUser(userId, address, destinations);
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
        TransitUser updatedUser = transitHelperDao.upsertUser(userId, address, destinations2);
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
        TransitUser user = transitHelperDao.upsertUser(userId, address, destinations);
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
        TransitUser user = transitHelperDao.upsertUser(userId, address, destinations);
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
        user = transitHelperDao.upsertUser(userId, address, destinations);
        Assert.assertEquals(destinations, user.getDestinations());
        userFromDb = transitHelperDao.getUser(userId);
        Assert.assertEquals(destinations, userFromDb.getDestinations());
        transitHelperDao.deleteUser(userId);
    }
}
