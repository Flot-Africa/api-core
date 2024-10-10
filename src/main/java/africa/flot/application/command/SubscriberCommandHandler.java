package africa.flot.application.command;

import africa.flot.domain.event.SubscriberCreatedEvent;
import africa.flot.domain.model.Subscriber;
import africa.flot.domain.model.Account;
import africa.flot.domain.model.enums.SubscriberStatus;
import africa.flot.domain.repository.AccountRepository;
import africa.flot.domain.repository.SubscriberRepository;
import africa.flot.infrastructure.messaging.QuarkusEventBus;
import africa.flot.infrastructure.security.AuthService;
import africa.flot.presentation.mapper.SubscriberMapper;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class SubscriberCommandHandler {

    @Inject
    SubscriberRepository subscriberRepository;
    @Inject
    AccountRepository accountRepository;
    @Inject
    SubscriberMapper mapper;
    @Inject
    QuarkusEventBus eventBus;
    @Inject
    AuthService authService;

    public Uni<UUID> handle(CreateSubscriberCommand command) {
        Subscriber subscriber = mapper.toEntity(command);
        return subscriberRepository.persist(subscriber)
                .onItem().transform(s -> {
                    eventBus.publish(new SubscriberCreatedEvent(s.id, s.email));
                    return s.id;
                });
    }

    public Uni<Void> validateSubscriber(UUID subscriberId) {
        return subscriberRepository.findById(subscriberId)
                .onItem().ifNotNull().invoke(subscriber -> subscriber.updateStatus(SubscriberStatus.VALIDE))
                .onItem().ifNotNull().transformToUni(subscriber -> subscriberRepository.merge(subscriber))
                .onItem().ifNotNull().transformToUni(this::createAccount)
                .replaceWithVoid();
    }

    private Uni<Account> createAccount(Subscriber subscriber) {
        Account account = new Account();
        account.subscriber = subscriber;
        account.username = subscriber.email;
        return authService.hashPassword(subscriber.email) // Assuming you have a method to generate a default password
                .onItem().transform(hashedPassword -> {
                    account.passwordHash = hashedPassword;
                    return account;
                })
                .chain(accountRepository::persist);
    }

    public Uni<Void> updateSubscriberStatus(UUID subscriberId, SubscriberStatus newStatus) {
        return subscriberRepository.findById(subscriberId)
                .onItem().ifNotNull().invoke(subscriber -> subscriber.updateStatus(newStatus))
                .onItem().ifNotNull().call(subscriberRepository::persist)
                .replaceWithVoid();
    }

    public Uni<Void> deactivateAccount(UUID subscriberId) {
        return accountRepository.findBySubscriberId(subscriberId)
                .onItem().ifNotNull().invoke(Account::deactivate)
                .onItem().ifNotNull().call(accountRepository::persist)
                .replaceWithVoid();
    }
}