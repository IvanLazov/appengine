package com.clouway.appengine.queries;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ivan Lazov <darkpain1989@gmail.com>
 */
public class LocalDatastoreTest {

  private final LocalServiceTestHelper service = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private DatastoreService datastoreService;

  @Before
  public void setUp() {
    service.setUp();
    datastoreService = DatastoreServiceFactory.getDatastoreService();
  }

  @After
  public void tearDown() {
    service.tearDown();
  }

  @Test
  public void saveQuery() {

    datastoreService.put(new Entity("Person"));
    datastoreService.put(new Entity("Person"));

    int savedEntities = datastoreService.prepare(new Query("Person")).countEntities(withLimit(10));

    assertThat(2, is(equalTo(savedEntities)));
  }

  @Test
  public void showPersonsUnderTwentyFiveYears() {

    List<Entity> personEntities = createListOfEntities(new String[]{"Ivan", "Georgi", "Adelin"}, new int[]{22, 23, 26});
    datastoreService.put(personEntities);

    Query query = new Query("Person");
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

      Entity person = new Entity("Person");
      person.setProperty("firstName", names[i]);
      person.setProperty("age", age[i]);

      persons.add(person);
    }

    return persons;
  }
}
