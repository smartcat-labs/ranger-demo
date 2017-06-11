# Integration testing with Ranger


**range** /reɪn(d)ʒ/ (noun): _the area of variation between upper and lower limits on a particular scale_.

## Intro

Filling the database with test data needed for integration testing can be quite tiresome, repetetive, and error prone process, beacause you have to create a lot of objects with different properties and values and to make sure that all the cases are covered. We wanted to remove the annoyance of creating test data manually (one object at the time), so we created [Ranger](https://github.com/smartcat-labs/ranger) - free and open source, Java library that allows for easy, declarative test data generation. You just have to describe your objects in regard to properties and values and Ranger will generate objects for you. You can basically do something like this: "create me a thousand instances of random Measurement objects such that exactly 10 are _owned_ by user 'charlie' and are from heart_beat_monitor sensor." Do you see how this can be usefull for integration testing? Here's a simple example.

Let's say that you work on IoT software and you have a query that fetches the newest 50 measurements for the given owner and sensor, which probably looks something like this:

```SQL
SELECT * FROM measurements WHERE owner = "charlie" AND sensor = "thermometer" SORT BY created DESC LIMIT 50;
``` 

or in MongoDB query language:

```mongodb
db.measurements.find( { owner : "charlie",  sensor : "thermometer" }).sort( { created : -1 }).limit(50);
```

You want to write an integration test for the query. The test data for the query should give the query a chance to be wrong. Which means that before we run the query, we want to have a bunch or users with bunch of sensor data in the database. That way we can make sure that the user and the sensor selection logic of the query (`WHERE owner = ? AND sensor = ?`) works as expected. Also, we want to have more than 50 measurements to test our `LIMIT 50` logic; and would be nice if data is not presorted in the database: just to cover `SORT BY created DESC` part of the query.

Ranger allows you to do that programmatically in a declarative manner.

##  The Assumptions about software engineering and testing

Why do I need this library? Why would I write complex database queries when I could just do the filtering and sorting in Java and that's easily testable? Why would I pay so much attention to testing the database queries anyway? Can't I assume that my database query just works, it's declarative afterall?

I will probably address these questions in a separate blog post. But just to be on the same page with all the readers, this is what we consider a good practice:

1. Fetching, filtering, and sorting of the data should be done by the database language (SQL, cql, mongodb query language, n1ql etc.) which is the most natural and most performing way to do it.
2. Since large part of business logic is written in a database language, that logic should be covered with automatic (integration or functional) tests. Why?
   - to make sure that complex queries do exactly what you want them to do.
   - to be able to do the refactoring and make changes and be sure that the query still works as expected
   - to be able to change/update the database driver (or even the database itself)
   - to document what the business logic does and what it does not (or what it must not do)

## Short intro to Ranger

For starters, let's just see how the Ranger is used and what is the idea behind it.

In order to test database queries, we need to create test data for each test case and put it in the database. This usually involves creating Java objects (or entities if you like), then inserting them into the database, then run the method/query that fetches the data and validates that the query is correctly written.

But, creating test data for each test case is repetetive and error prone. You have to create each object separately, you have to create several expected objects, then several objects that should not be retrieved and so on. This can be quite verbose, time consuming and hard to maintain afterwards.

Therefore, we created and open-sourced Ranger - tool that helps us to declaratively generate any number of objects programatically. You just have to describe how the objects look like, what are allowed values for each attribute, and Ranger will use reflection to set the randomly chosen value to the field. Before proceeding, if you already haven't, please take a look at [Ranger's github repository](https://github.com/smartcat-labs/ranger). I'll wait here.

Okay, you're back. So, as you've just seen, with Ranger, we can describe how our data looks like. As we said, we'll use IoT app example, where core part of our system is `Measurement` class:

```java
public class Measurement {
  
  private String id;
  private long created;
  private String sensor;
  private String owner;
  private long measuredValue;
  private short version;
  private Date lastChanged;
  private boolean active;

// getters and setters
}
```

Ranger can help us with creating test `Measurement` objects, so we can populate the database before running an integration test. With Ranger we can create _recipes_ in which we declare possible values for each field. For example we can create a recipe like this:
generate 50 objects of Measurement class such that `owner` is someone from this set ("alex", "bob", "charlie"), and `created` is a timestamp between [1496300000, 196400000), and `sensor` can be something from this set ("heart-rate-monitor", "accelerometer", "hygrometer", "thermometer") and so on. The recipe looks like this:

```java
ObjectGenerator<Measurement> measurementGenerator = new ObjectGenerator.Builder<Measurement>(Measurement.class)
    .withValues("owner", "alex", "bob", "charlie")
    .withValues("created", 1496300000L, 196400000L)
    .withValues("sensor",  "heart-rate-monitor", "accelerometer", "hygrometer", "thermometer")
    .toBeGenerated(50)
    .build();
List<Measurement> measurements = measurementGenerator.generate();
```

Simple, isn't it? We can also combine the recipes so we can make data abide certain rules like: create a thousand Measurement objects for a bunch of users and with bunch of sensors, but only 100 them will be from user "charlie". In other words, we want 900 measurement object for non-charlie users and 100 for user "charlie":

```java
ObjectGenerator<Measurement> nonCharlieMeasurementGenerator = new ObjectGenerator.Builder<Measurement>(Measurement.class)
    .withValues("owner", "alex", "bob", "david", "emma")
    .withValues("created", 1496300000L, 196400000L)
    .withValues("sensor",  "heart-rate-monitor", "accelerometer", "hygrometer", "thermometer")
    .toBeGenerated(900)
    .build();

ObjectGenerator<Measurement> charlieMeasurementGenerator = new ObjectGenerator.Builder<Measurement>(Measurement.class)
    .withValues("owner", "charlie")
    .withValues("created", 1496300000L, 196400000L)
    .withValues("sensor",  "heart-rate-monitor", "accelerometer", "hygrometer", "thermometer")
    .toBeGenerated(100)
    .build();
```

And now we just pass `nonCharlieMeasurementGenerator` and `charlieMeasurementGenerator` to `AggregatedObjectGenerator<Measurement>` in order to generate objects from both generators:

```java
AggregatedObjectGenerator<Measurement> aggregatedObjectGenerator = new AggregatedObjectGenerator.Builder<Measurement>()
    .withObjectGenerator(nonCharlieMeasurementGenerator)
    .withObjectGenerator(charlieMeasurementGenerator)
    .build();

List<Measurement> measurements = aggregatedObjectGenerator.generateAll();
```

Voila!

## Ranger - real world example

Now that we are familiar with the basic building blocks for Ranger library, let's try it out with a real world example.

We'll reuse the IoT software example. Say that we're building IoT application that collects measurements from bunch of sensors and provides analytics and alerting based on the stored data. And let's say that we've created a database query for fetching the newest `n` results for a particular user and sensor. We're using MongoDB in this example, for the simplicity. So, somewhere in our code, we have a database query that looks something like this:

```mongo
db.measurements.find( { owner : ?,  sensor : ?}).sort( { created : -1 }).limit(50);
```

or in case of SQL:

```SQL
select * from measurements where owner = ? and sensor = ? sort by created desc limit 50;
```

That's quite common thing to do with time series data. And, or course, we want to create automated tests for that query.
For the simplicity I will put all the test cases in one test. If you like to do it by the book you would probably create a separate, orthogonal test for each case.

```java
@Test
public void should_return_newest_50_measurements_for_selected_user_and_sensor() {
  createTestMeasurements();
  List<Measurement> result = measurementService.getNewestMeasurementsForUserAndSensor("charlie", "thermometer", 50);
    
  // validate that 50 results are returned
  Assert.assertEquals(50, result.size());
    
  // validate that user is "charlie"
  result.forEach(measurement -> Assert.assertEquals("charlie", measurement.getOwner()));
    
  // validate that sensor is "thermometer"
  result.forEach(measurement -> Assert.assertEquals("thermometer", measurement.getSensor()));
    
  // validate that created is between 1000 and 1100
  result.forEach(measurement -> 
    Assert.assertTrue(measurement.getCreated() >= 1000 && measurement.getCreated() < 1100));
    
  // validate that data is returned in descending order by "created" timestamp
  long previousTimestamp = result.get(0).getCreated();
  for (Measurement m : result) {
    Assert.assertTrue(m.getCreated() <= previousTimestamp);
    previousTimestamp = m.getCreated();
  }
}
```

The test data is created in `createTestMeasurements()`.

If we take a look at the query under test `db.measurements.find( { owner : ?,  sensor : ?}).sort( { created : -1 }).limit(50)` and its wrapper method `measurementService.getNewestMeasurementsForUserAndSensor("charlie", "thermometer", 50)` we see there are several components, or dimensions of the query. First, there are usernames and sensor type. Then, there is a timestamp ("created" field); and in the end there is a limit (we want only last `n` measurements);

But, in order to test the correctness of the query, it is of critical importance to cover every dimenssion (property) of the query. Why? Because we want to give our query a chance to be incorrect. If we only had data for one user, we would not see if our user selection part of the query works. Or if we only have one sensor we can't know that the sensor selection part of the query works correctly. And so on.

How to create test data for the query?

Let's take a look at the query once again:

```SQL
SELECT * FROM measurements WHERE owner = ? AND senor = ? SORT BY created DESC LIMIT 50;
```

I usually draw a table like this, when I need to write a test to be sure that the test data is correctly created:

```
 # |    username     |    sensor             |        created         |
===|=================|=======================|========================| 
 1 |    "charlie"    |    "thermometer"      |        [1000,1100)     | expected result
---|-----------------|-----------------------|------------------------|
 2 | other usernames |    "thermometer"      |        [1000,1100)     | ✖ username, ✓ sensor, ✓ created
---|-----------------|-----------------------|------------------------|
 3 |    "charlie"    |     other sensors     |        [1000,1100)     | ✓ username, ✖ sensor, ✓ created
---|-----------------|-----------------------|------------------------|
 4 |    "charlie"    |    "thermometer"      |        [800,1000)      | ✓ username, ✓ sensor, ✖ created
---|-----------------|-----------------------|------------------------|
```

Okay, so, we need:
1. expected result - 50 objects should have username : "charlie", sensor : "thermometer" and be created between 1000 (inclusive) and 1100 (exclusive).
2. results with wrong username - bunch of objects (say 1000) will be the same as 1. but with different username
3. results with wrong sensor - bunch of objects (say 1000) will be the same as 1. but with different sensor
4. results with wrong timestamp - bunch of objects (say 1000) will be the same as 1. but created property's values is bellow 1000.

By increasing the number of test objects/records, we're reducing the chances of the test passing only because some strange coincidence occured. For example, if you have only three objects in the database and you didn't pay attention, probability that they are already sorted by timestamp in the database are pretty high. In that case, the test will pass even if the sorting part of the query is missing. But if the timestamp is randomly generated, and we have 50 objects in the database, the probability of having a presorted records in the database is quite low (1 in 50!). That way we're reducing the probability of making a mistake in tests.

With Ranger, once you declare how the data looks like, you can create any number of objects. And I always prefer creating thousands of objects for integration testing. That way I'm increasing the probability of covering some edge case that I failed to notice.

In addition to this, we want to give meaningfull values to all the attributes. It's hard to debug and spot errors in a bunch of jibberish strings. For example, if we're creating sensors, we want them to have the names of the real sensors. We want that in integration tests, because it is much easier to reason if the values are real, thus it's easier to debug and maintain the tests later on.

First, we will create builder (or recipe, description) for newest 50 thermometer measurements for user 'charlie':

```java
// expected result
ObjectGenerator<Measurement> newestMeasurementsForCharlieAndThermometer = new ObjectGenerator.Builder<Measurement>(Measurement.class)
  .withValues("owner", "charlie")
  .withRanges("created", 1000L, 1100L)
  .withValues("sensor", "thermometer")
  .withValues("version", (short) 1)
  .toBeGenerated(50)
  .build();
```

In this part we declared how the expected result looks like. Once we fetch the data in the test, we can check that the value of `created` property is in the correct range [1000, 1100). Since we're in control of the test data, we just have to make sure that we don't create newer data for this particular user.

Have you heard of those nasty [off-by-one errors](https://en.wikipedia.org/wiki/Off-by-one_error)? Of course you have. Well, you can forget about those now, because Ranger will always create corner cases for you. What does that mean? That means that if you declare range of values for a property, it will always include range boundaries in the generated data. The range is defined with inclusive begining and exclusive end. In this case when I declare `.withRanges("created", 15000L, 15050L)` that means that at least one instance of the generated Measurement objects will have "created" with value 15000 (the begining of the range) and at least one instance will have "created" with value 15049 (the end of the range).

Okay, so far we have declared a set of expected results. Now we need noise data - the overlapping data that shouldn't be part of the returned data set. First, let's create data for other users which will contain data from all the sensors, including the "thermometer" and will include newer and older measurements:

```java
// ✖ username, ✓ sensor, ✓ created
ObjectGenerator<Measurement> overlappingMeasurementTimestampOtherOwners = new ObjectGenerator.Builder<Measurement>(Measurement.class)
  .withValues("owner", "alice", "bob", "david", "emma", "flint") // note: no "charlie"
  .withRanges("created", 1000L, 1100L)
  .withValues("sensor",  "heart-rate-monitor", "accelerometer", "hygrometer", "thermometer")
  .toBeGenerated(1000)
  .build();
```

Now, let's create measurements for "charlie" that are newer than what we defined in the expected result, but from the wrong sensor:

```java
// ✓ username, ✖ sensor, ✓ created
ObjectGenerator<Measurement> newerDataForCharlieAndNonThermometer = new ObjectGenerator.Builder<Measurement>(Measurement.class)
  .withValues("owner", "charlie")
  .withRanges("created", 1100L, 1200L)
  .withValues("sensor", "heart-rate-monitor", "accelerometer", "hygrometer") // note: no thermometer
  .toBeGenerated(1000)
  .build();
```

And the final step is to create some Measurement objects for the correct user ("charlie") and correct sensor ("thermometer") but the timestamp is older than in the 50 results that are part of the expected result set (newestMeasurementsForCharlieAndThermometer):

```java
// ✓ username, ✓ sensor, ✖ created
ObjectGenerator<Measurement> oldDataForCharlie = new ObjectGenerator.Builder<Measurement>(Measurement.class)
  .withValues("owner", "charlie")
  .withRanges("created", 500L, 1000L) // note: timestamp is <1000
  .withValues("sensor", "heart-rate-monitor", "accelerometer", "hygrometer", "thermometer")
  .toBeGenerated(1000)
  .build();
```

Now, if take a look at the whole method for creation of test data:

```java
ObjectGenerator<Measurement> newestMeasurementsForCharlieAndThermometer = new ObjectGenerator.Builder<Measurement>(Measurement.class)
  .withValues("owner", "charlie")
  .withRanges("created", 1000L, 1100L)
  .withValues("sensor", "thermometer")
  .withValues("version", (short) 1)
  .toBeGenerated(50).build();
    
ObjectGenerator<Measurement> overlappingMeasurementTimestampOtherOwners = new ObjectGenerator.Builder<Measurement>(Measurement.class)
  .withValues("owner", "alice", "bob", "david", "emma", "flint") // note: no "charlie"
  .withRanges("created", 1000L, 1100L)
  .withValues("sensor",  "heart-rate-monitor", "accelerometer", "hygrometer", "thermometer")
  .toBeGenerated(1000).build();

ObjectGenerator<Measurement> newerDataForCharlieAndNonThermometer = new ObjectGenerator.Builder<Measurement>(Measurement.class)
  .withValues("owner", "charlie")
  .withRanges("created", 1100L, 1200L)
  .withValues("sensor", "heart-rate-monitor", "accelerometer", "hygrometer")
  .toBeGenerated(1000).build();

ObjectGenerator<Measurement> oldDataForCharlie = new ObjectGenerator.Builder<Measurement>(Measurement.class)
  .withValues("owner", "charlie")
  .withRanges("created", 500L, 1000L)
  .withValues("sensor", "heart-rate-monitor", "accelerometer", "hygrometer", "thermometer")
  .toBeGenerated(1000).build();

AggregatedObjectGenerator<Measurement> aggregatedObjectGenerator = new AggregatedObjectGenerator.Builder<Measurement>()
  .withObjectGenerator(newestMeasurementsForCharlieAndThermometer)
  .withObjectGenerator(overlappingMeasurementTimestampOtherOwners)
  .withObjectGenerator(oldDataForCharlie)
  .withObjectGenerator(newerDataForCharlieAndNonThermometer)
  .build();

List<Measurement> measurements = aggregatedObjectGenerator.generateAll();
mongoOps.insertAll(measurements);
```

We can see that test data declared like this is:
- relatively easy to reason about
- the data set _description_ fits into eye view
- each data set component is (or at least could be) meaningfully named - newestMeasurementsForCharlieAndThermometer (as long as you love how Java people name things :D )

## Conclusion

Honestly, I was really surprised that something like this did not exist. On every project I worked on I had to create test data from the ground up, one object at the time. That lead to errors and frustration. With Ranger, you still have to pay attention when creating test data, but the creation is much easier to do it declaratively.

What do you think about this? Would you find it useful? If you have any feature ideas, please create an issue on [Ranger's github repo](https://github.com/smartcat-labs/ranger/issues).

If you're interested and want to try Ranger out, you can take a look at the [demo application](https://github.com/smartcat-labs/ranger-demo) which uses embedded MongoDB for integration testing.

P.S. We're currently working on making Ranger run from the command line. That way we'll be able to describe our  data set in a yaml configuration file and just fill the database or API with generated objects/entities/requests. This will come in handy when you want to quickly populate the database with some meaningful values.

