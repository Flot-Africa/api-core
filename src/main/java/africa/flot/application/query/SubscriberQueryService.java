package africa.flot.application.query;

import africa.flot.domain.repository.SubscriberRepository;
import africa.flot.presentation.dto.query.SubscriberDTO;
import africa.flot.presentation.mapper.SubscriberMapper;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class SubscriberQueryService {

    private final SubscriberRepository repository;

    @Inject
    SubscriberMapper mapper;
    @Inject
    public SubscriberQueryService(SubscriberRepository repository) {
        this.repository = repository;
    }

    public Uni<SubscriberDTO> getSubscriberById(UUID id) {
        return repository.findById(id)
                .onItem().ifNotNull().transform(mapper::toDTO);
    }

    public Uni<List<SubscriberDTO>> getAllSubscribers() {
        return repository.listAll()  // Changé de findAll() à listAll()
                .onItem().transform(list -> list.stream().map(mapper::toDTO).toList());
    }

    public Uni<SubscriberDTO> getSubscriberByEmail(String email) {
        return repository.findByEmail(email)
                .onItem().ifNotNull().transform(mapper::toDTO);
    }
}