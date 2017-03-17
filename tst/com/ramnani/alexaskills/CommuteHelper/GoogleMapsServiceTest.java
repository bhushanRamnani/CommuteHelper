package com.ramnani.alexaskills.CommuteHelper;

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
    public void getNextBusToWork_Sanity() {
        List<TransitSuggestion> suggestions = service.getNextTransitToDestination(
                "bus", "2400 Boyer Ave E, Seattle, WA 98112", "1918 8th Ave, Seattle, WA 98101");
        assertNotNull(suggestions);
        int leavingStartTime = suggestions.get(0).getLeavingTimeInSeconds();
        assertTrue(leavingStartTime >= 0);
        assertTrue(suggestions.size() > 0);
    }

    @Test
    public void getNextTransitToPlace_Sanity() {
        String address = service.getAddressOfPlace("999 Belmont terr Sunnyvale California");
        assertNotNull(address);
    }
}
