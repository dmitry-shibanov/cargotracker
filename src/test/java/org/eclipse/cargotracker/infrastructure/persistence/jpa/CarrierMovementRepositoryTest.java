package org.eclipse.cargotracker.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.cargotracker.Deployments.addApplicationBase;
import static org.eclipse.cargotracker.Deployments.addDomainModels;
import static org.eclipse.cargotracker.Deployments.addDomainRepositories;
import static org.eclipse.cargotracker.Deployments.addExtraJars;
import static org.eclipse.cargotracker.Deployments.addInfraBase;
import static org.eclipse.cargotracker.Deployments.addInfraPersistence;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import org.eclipse.cargotracker.IntegrationTests;
import org.eclipse.cargotracker.application.util.RestConfiguration;
import org.eclipse.cargotracker.application.util.SampleDataGenerator;
import org.eclipse.cargotracker.domain.model.location.Location;
import org.eclipse.cargotracker.domain.model.location.SampleLocations;
import org.eclipse.cargotracker.domain.model.voyage.CarrierMovement;
import org.eclipse.cargotracker.domain.model.voyage.SampleVoyages;
import org.eclipse.cargotracker.domain.model.voyage.Schedule;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.eclipse.cargotracker.domain.model.voyage.VoyageRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@Category(IntegrationTests.class)
public class CarrierMovementRepositoryTest {
  private static final Logger LOGGER =
      Logger.getLogger(CarrierMovementRepositoryTest.class.getName());
  @Inject VoyageRepository voyageRepository;
  @PersistenceContext EntityManager entityManager;
  @Inject UserTransaction utx;
  String voyageNumberIdString = "007";
  Voyage voyage;
  Location from = SampleLocations.HONGKONG;
  Location to = SampleLocations.CHICAGO;
  LocalDateTime fromDate = LocalDateTime.now();
  LocalDateTime toDate = LocalDateTime.now();

  @Deployment
  public static WebArchive createDeployment() {
    WebArchive war = ShrinkWrap.create(WebArchive.class, "test-CarrierMovementRepositoryTest.war");

    addExtraJars(war);
    addDomainModels(war);
    addDomainRepositories(war);
    addInfraBase(war);
    addInfraPersistence(war);
    addApplicationBase(war);

    war.addClass(RestConfiguration.class);
    war.addClass(SampleDataGenerator.class)
        .addClass(SampleLocations.class)
        .addClass(SampleVoyages.class)
        // add persistence unit descriptor
        .addAsResource("test-persistence.xml", "META-INF/persistence.xml")

        // add web xml
        .addAsWebInfResource("test-web.xml", "web.xml")

        // add Wildfly specific deployment descriptor
        .addAsWebInfResource(
            "test-jboss-deployment-structure.xml", "jboss-deployment-structure.xml");

    LOGGER.log(Level.INFO, "War deployment: {0}", war.toString(true));

    return war;
  }

  public void startTransaction() throws Exception {
    utx.begin();
    entityManager.joinTransaction();
  }

  public void commitTransaction() throws Exception {
    LOGGER.log(Level.INFO, "UserTransaction status is: {0}", utx.getStatus());
    if (utx.getStatus() == Status.STATUS_ACTIVE) {
      utx.commit();
    }
  }

  @Before
  public void setup() throws Exception {
    startTransaction();
    voyage =
        new Voyage(
            new VoyageNumber(voyageNumberIdString),
            new Schedule(
                Collections.singletonList(new CarrierMovement(from, to, fromDate, toDate))));
    this.entityManager.persist(voyage);
    this.entityManager.flush();
    commitTransaction();
  }

  @Test
  public void testFind() throws Exception {
    startTransaction();
    Voyage result = voyageRepository.find(new VoyageNumber(voyageNumberIdString));
    assertThat(result).isNotNull();
    assertThat(result.getVoyageNumber().getIdString()).isEqualTo(voyageNumberIdString);

    var movements = result.getSchedule().getCarrierMovements();
    assertThat(movements).hasSize(1);

    var m = movements.get(0);
    assertThat(m.getDepartureLocation()).isEqualTo(from);
    assertThat(m.getArrivalLocation()).isEqualTo(to);
    assertThat(m.getDepartureTime().truncatedTo(ChronoUnit.SECONDS))
        .isEqualTo(fromDate.truncatedTo(ChronoUnit.SECONDS));
    assertThat(m.getArrivalTime().truncatedTo(ChronoUnit.SECONDS))
        .isEqualTo(toDate.truncatedTo(ChronoUnit.SECONDS));
    commitTransaction();
  }
}
