package com.clouway.appengine.queries;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.appengine.api.datastore.FetchOptions.Builder.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ivan Lazov <darkpain1989@gmail.com>
 */
public class LocalDatastoreTest {

  private final LocalServiceTestHelper service = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig().
          setDefaultHighRepJobPolicyUnappliedJobPercentage(100));

  private DatastoreService datastoreService;
  private Key root;

  @Before
  public void setUp() {
    service.setUp();
    root = KeyFactory.createKey("Root", "root");
    datastoreService = DatastoreServiceFactory.getDatastoreService();
  }

  @After
  public void tearDown() {
    service.tearDown();
  }

  @Test
  public void saveQuery() {

    datastoreService.put(new Entity("Person", root));
    datastoreService.put(new Entity("Person", root));

    Query query = new Query("Person");
    query.setAncestor(root);

    PreparedQuery preparedQuery = datastoreService.prepare(query);

    int savedEntities = preparedQuery.countEntities(withLimit(10));

    assertThat(2, is(equalTo(savedEntities)));
  }

  @Test
  public void sampleTest() {

    Entity person = new Entity("Person", root);

    datastoreService.put(person);

    Query query = new Query("Person");
    query.setAncestor(root);

    PreparedQuery preparedQuery = datastoreService.prepare(query);
    Entity result = preparedQuery.asList(withDefaults()).get(0);

    assertThat(result, is(equalTo(person)));
  }

  @Test
  public void showPersonsUnderTwentyFiveYears() {

    List<Entity> personEntities = createListOfEntities(new String[]{"Ivan", "Georgi", "Adelin"}, new int[]{22, 23, 26});
    datastoreService.put(personEntities);

    Query query = new Query("Person");
    query.setAncestor(root);

    query.setFilter(new Query.FilterPredicate("age", Query.FilterOperator.LESS_THAN, 25));
    PreparedQuery preparedQuery = datastoreService.prepare(query);

    List<Entity> returnedPersonEntities = preparedQuery.asList(withLimit(10));

    assertThat(2, is(equalTo(returnedPersonEntities.size())));
    assertThat(personEntities.get(0), is(equalTo(returnedPersonEntities.get(0))));
    assertThat(personEntities.get(1), is(equalTo(returnedPersonEntities.get(1))));
  }

  private List<Entity> createListOfEntities(String[] names, int[] age) {

    List<Entity> persons = new ArrayList<Entity>();

    for (int i = 0; i < names.length; i++) {

      Entity person = new Entity("Person", root);
      person.setProperty("firstName", names[i]);
      person.setProperty("age", age[i]);

      persons.add(person);
    }

    return persons;
  }

  @Test
  public void ancestorQuery() {

    // Create Entities
    Entity person = new Entity("Person", "tom");

    Entity weddingPhoto = new Entity("Photo", person.getKey());
    Entity babyPhoto = new Entity("Photo", person.getKey());
    Entity dancePhoto = new Entity("Photo", person.getKey());

    Entity campingPhoto = new Entity("Photo");

    // Save Entities
    datastoreService.put(Arrays.asList(person, weddingPhoto, babyPhoto, dancePhoto, campingPhoto));

    // Create Query
    Query userPhotosQuery = new Query("Photo");
    userPhotosQuery.setAncestor(person.getKey());

    List<Entity> results = datastoreService.prepare(userPhotosQuery).asList(withDefaults());

    assertThat(3, is(equalTo(results.size())));
    assertThat(weddingPhoto, is(equalTo(results.get(0))));
    assertThat(babyPhoto, is(equalTo(results.get(1))));
    assertThat(dancePhoto, is(equalTo(results.get(2))));
  }

  @Test
  public void kindlessAncestorQuery() {

    Entity person = new Entity("Person", "tom");

    Entity weddingPhoto = new Entity("Photo", person.getKey());
    weddingPhoto.setProperty("imageUrl", "images/weddingPhoto");

    Entity weddingVideo = new Entity("Video", person.getKey());
    weddingVideo.setProperty("videoUrl", "videos/weddingVideo");

    datastoreService.put(Arrays.asList(person, weddingPhoto, weddingVideo));

    Query userMediaQuery = new Query();
    userMediaQuery.setAncestor(person.getKey());

    // Ancestor queries return ancestors by default.
    // This filter excludes the ancestor from query result
    userMediaQuery.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, Query.FilterOperator.GREATER_THAN, person.getKey()));

    // Returns both weddingPhoto and weddingVideo even though they are
    // different entity kinds
    List<Entity> result = datastoreService.prepare(userMediaQuery).asList(withDefaults());
    assertThat(2, is(equalTo(result.size())));
  }

  @Test
  public void getTheFirstTwoHighestPersons() {

    Entity person = new Entity("Person", root);
    person.setProperty("height", 1.80);

    Entity person2 = new Entity("Person", root);
    person2.setProperty("height", 1.86);

    Entity person3 = new Entity("Person", root);
    person3.setProperty("height", 1.95);

    Entity person4 = new Entity("Person", root);
    person4.setProperty("height", 2.00);

    Entity person5 = new Entity("Person", root);
    person5.setProperty("height", 2.10);

    datastoreService.put(Arrays.asList(person, person2, person3, person4, person5));

    Query query = new Query("Person");
    query.setAncestor(root);
    query.addSort("height", Query.SortDirection.DESCENDING);

    PreparedQuery preparedQuery = datastoreService.prepare(query);

    List<Entity> result = preparedQuery.asList(withLimit(2));

    assertThat(2, is(equalTo(result.size())));
    assertThat(person5.getProperty("height"), is(equalTo(result.get(0).getProperty("height"))));
    assertThat(person4.getProperty("height"), is(equalTo(result.get(1).getProperty("height"))));
  }

  @Test
  public void unindexedPropertyQuery() {

    Key companyKey = KeyFactory.createKey("Company", "companyKey");

    Entity tom = new Entity("Person", "tom", companyKey);
    tom.setProperty("age", 32);

    Entity lucy = new Entity("Person", "lucy", companyKey);
    lucy.setUnindexedProperty("age", 28);

    datastoreService.put(Arrays.asList(tom, lucy));

    Query query = new Query("Person");
    query.setAncestor(companyKey);
    query.setFilter(new Query.FilterPredicate("age", Query.FilterOperator.GREATER_THAN, 25));

    // Returns tom but not lucy because her age is unindexed
    List<Entity> result = datastoreService.prepare(query).asList(withDefaults());

    assertThat(1, is(equalTo(result.size())));
  }

  @Test
  public void projectionQuery() {

    Key guestbookKey = KeyFactory.createKey("Guestbook", "guestbookKey");

    Entity person = new Entity("Person", guestbookKey);
    person.setProperty("name", "Ivan");
    person.setProperty("balance", 100.0);

    datastoreService.put(person);

    Query projectionQuery = new Query("Person", guestbookKey);
    projectionQuery.addProjection(new PropertyProjection("name", String.class));
    projectionQuery.addProjection(new PropertyProjection("balance", Double.class));

    List<Entity> result = datastoreService.prepare(projectionQuery).asList(withDefaults());

    assertThat(person.getProperty("name"), is(equalTo(result.get(0).getProperty("name"))));
    assertThat(person.getProperty("balance"), is(equalTo(result.get(0).getProperty("balance"))));
  }
}
