package org.eclipse.cargotracker.infrastructure.routing;

import org.eclipse.cargotracker.domain.model.cargo.Itinerary;
import org.eclipse.cargotracker.domain.model.cargo.Leg;
import org.eclipse.cargotracker.domain.model.cargo.RouteSpecification;
import org.eclipse.cargotracker.domain.model.location.LocationRepository;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.eclipse.cargotracker.domain.model.voyage.VoyageRepository;
import org.eclipse.cargotracker.domain.service.RoutingService;
import org.eclipse.cargotracker.infrastructure.routing.client.GraphTraversalResourceClient;
import org.eclipse.pathfinder.api.TransitEdge;
import org.eclipse.pathfinder.api.TransitPath;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Our end of the routing service. This is basically a data model translation layer between our
 * domain model and the API put forward by the routing team, which operates in a different context
 * from us.
 */
@Stateless
public class ExternalRoutingService implements RoutingService {

    private static final Logger LOGGER = Logger.getLogger(ExternalRoutingService.class.getName());

    private final LocationRepository locationRepository;

    private final VoyageRepository voyageRepository;

    private final GraphTraversalResourceClient graphTraversalResource;

    @Inject
    public ExternalRoutingService(
            LocationRepository locationRepository,
            VoyageRepository voyageRepository,
            GraphTraversalResourceClient graphTraversalResource) {
        this.locationRepository = locationRepository;
        this.voyageRepository = voyageRepository;
        this.graphTraversalResource = graphTraversalResource;
    }

    //    @PostConstruct
    //    public void init() {
    //        // this.graphTraversalResource = new GraphTraversalResourceClient();
    //        //graphTraversalResource.register(new MoxyJsonFeature()).register(new
    // JsonMoxyConfigurationContextResolver());
    //    }

    @Override
    public List<Itinerary> fetchRoutesForSpecification(RouteSpecification routeSpecification) {
        // The RouteSpecification is picked apart and adapted to the external API.
        String origin = routeSpecification.getOrigin().getUnLocode().getIdString();
        String destination = routeSpecification.getDestination().getUnLocode().getIdString();

        List<TransitPath> transitPaths =
                this.graphTraversalResource.findShortestPath(origin, destination);

        // The returned result is then translated back into our domain model.
        List<Itinerary> itineraries = new ArrayList<>();

        // Use the specification to safe-guard against invalid itineraries
        transitPaths.stream()
                .map(this::toItinerary)
                .forEach(
                        itinerary -> {
                            if (routeSpecification.isSatisfiedBy(itinerary)) {
                                itineraries.add(itinerary);
                            } else {
                                LOGGER.log(
                                        Level.FINE,
                                        "Received itinerary that did not satisfy the route specification: {0}",
                                        itinerary);
                            }
                        });

        return itineraries;
    }

    private Itinerary toItinerary(TransitPath transitPath) {
        List<Leg> legs =
                transitPath.getTransitEdges().stream()
                        .map(this::toLeg)
                        .collect(Collectors.toList());
        return new Itinerary(legs);
    }

    private Leg toLeg(TransitEdge edge) {
        return new Leg(
                voyageRepository.find(new VoyageNumber(edge.getVoyageNumber())),
                locationRepository.find(new UnLocode(edge.getFromUnLocode())),
                locationRepository.find(new UnLocode(edge.getToUnLocode())),
                edge.getFromDate(),
                edge.getToDate());
    }
}
