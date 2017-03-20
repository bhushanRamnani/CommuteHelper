# CommuteHelper (a.k.a TransitCompanion)

An Alexa skill that helps the user with transit information from home to work.

You can ask questions like,
"When's my next bus to work?" or "When's my next train to work?"

Example response: "Your next bus is bus number 49. It'll arrive in 17 minutes. It'll take you 
16 minutes to walk to the bus location. I recommend you leave now."

Then you can get more information on the transit option suggested by TransitCompanion, such as:

"What's the duration?"
Example Response: "The transit duration is 20 minutes."

"When will I reach?"
Example Response: "You will reach by 9AM."

"Give me directions to the bus stop"
Example Response: "First walk for 0.5 mile till 8th Avenue and 66th street. 
Then take a left on 8th avenue and walk for 0.5 miles"


User Setup:
In order to provide transit information, the user needs to go through a one time setup process.
During this setup process, s/he needs to provide her/his home address and work address.
This input is taken using a conversational speech model and verified against google maps database, in order
to make the setup process smooth and accurate.

All user address information is encrypted ar rest using client side encryption and AWS Key Management Service

Transit types include bus, rail, train, metro, subway, tram, monorail, heavy rail, commuter train
, high speed train, bus, intercity bus, trolley bus, share taxi, ferry, cable car, cable, gondola, funicular
