package com.clouway.appengine.queries;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ivan Lazov <darkpain1989@gmail.com>
 */
public class TransactionsTest {

  private final LocalServiceTestHelper service = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig().
          setDefaultHighRepJobPolicyUnappliedJobPercentage(1));

  private DatastoreService datastore;

  @Before
  public void setUp() {
    service.setUp();
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @After
  public void tearDown() {
    service.tearDown();
  }

  @Test
  public void updateFieldInEntity() throws EntityNotFoundException {

    Entity person = new Entity("Person");
    person.setProperty("firstName", "Ivan");
    person.setProperty("age", 22l);

    Key key = datastore.put(person);

    // Start transaction
    Transaction transaction = datastore.beginTransaction();
    try {

      Entity result = datastore.get(key);
      result.setProperty("age", 30l);

      datastore.put(result);

      transaction.commit();

    } finally {
      if (transaction.isActive()) {
        transaction.rollback();
      }
    }

    Entity result = datastore.get(key);
    assertThat("Ivan", is(equalTo(result.getProperty("firstName"))));
    assertThat(30l, is(equalTo(result.getProperty("age"))));
  }

  @Test
  public void transactionsOnVariusTypesOfEntities() throws EntityNotFoundException {

    Entity person = new Entity("Person", "tom");
    datastore.put(person);

    // Transaction on root entities
    Transaction transaction = datastore.beginTransaction();

    Entity tom = datastore.get(person.getKey());
    tom.setProperty("age", 40);
    datastore.put(tom);
    transaction.commit();

    // Transaction on child entities
    transaction = datastore.beginTransaction();
    tom = datastore.get(person.getKey());

    // Create a Photo entity that is a child of Person entity named "tom"
    Entity photo = new Entity("Photo", tom.getKey());
    photo.setProperty("photoUrl", "images/photo");
    datastore.put(photo);
    transaction.commit();

    // Transaction on entities in different entity groups
    transaction = datastore.beginTransaction();

    Entity photoNotChild = new Entity("Photo");
    photoNotChild.setProperty("photoUrl", "images/photo");
    datastore.put(photoNotChild);
    transaction.commit();
  }

  @Test
  public void crossGroupTransactions() {

    TransactionOptions transactionOptions = TransactionOptions.Builder.withXG(true);
    Transaction transaction = datastore.beginTransaction(transactionOptions);

    Entity a = new Entity("A");
    datastore.put(a);

    Entity b = new Entity("B", a.getKey());
    datastore.put(b);

    Entity c = new Entity("C", a.getKey());
    datastore.put(c);

    Entity d = new Entity("D");
    datastore.put(d);

    Entity e = new Entity("E");
    datastore.put(e);

    Entity f = new Entity("F");
    datastore.put(f);

    Entity z = new Entity("Z");
    datastore.put(z);

    transaction.commit();
  }
}
