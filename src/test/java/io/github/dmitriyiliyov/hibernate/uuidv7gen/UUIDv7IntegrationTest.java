package io.github.dmitriyiliyov.hibernate.uuidv7gen;

import io.github.dmitriyiliyov.hibernate.uuidv7gen.app.*;
import io.github.dmitriyiliyov.hibernate.uuidv7gen.support.HibernateSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.github.dmitriyiliyov.hibernate.uuidv7gen.support.Uuids.assertIsUuidV7;
import static io.github.dmitriyiliyov.hibernate.uuidv7gen.support.Uuids.timestampMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Drives {@link UUIDv7} through a real Hibernate {@code SessionFactory} on H2: entities are mapped,
 * the schema is exported, and rows go through actual transactions.
 */
class UUIDv7IntegrationTest {

    private static final String CUSTOMER = "Ada";
    private static final int THREADS = 8;
    private static final int PER_THREAD = 25;

    private ShopApplication tested;

    @BeforeEach
    void startApplication() {
        SequencedUuidGenerator.resetInvocations();
        tested = new ShopApplication(HibernateSupport.buildSessionFactory(
                Customer.class, Order.class, AuditRecord.class));
    }

    @AfterEach
    void stopApplication() {
        if (tested != null) {
            tested.close();
        }
    }

    @Test
    @DisplayName("IT persist() when the entity is new should assign a UUIDv7 before the insert is issued")
    void persist_whenEntityIsNew_shouldAssignUuidV7BeforeInsertIsIssued() {
        // given
        long before = Instant.now().toEpochMilli();

        // when
        UUID result = tested.registerCustomer(CUSTOMER);

        // then
        long after = Instant.now().toEpochMilli();
        assertThat(result).isNotNull();
        assertIsUuidV7(result);
        assertThat(timestampMillis(result)).isBetween(before, after);
    }

    @Test
    @DisplayName("IT persist() when the entity is new should store the generated identifier in the table")
    void persist_whenEntityIsNew_shouldStoreGeneratedIdentifierInTable() {
        // when
        UUID result = tested.registerCustomer(CUSTOMER);

        // then
        assertThat(tested.readCustomerIdViaSql(CUSTOMER)).isEqualTo(result);
        Customer reloaded = tested.findCustomer(result);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getId()).isEqualTo(result);
        assertThat(reloaded.getName()).isEqualTo(CUSTOMER);
    }

    @Test
    @DisplayName("IT persist() when several rows are inserted should assign a distinct identifier to each of them")
    void persist_whenSeveralRowsAreInserted_shouldAssignDistinctIdentifierToEachOfThem() {
        // given
        int rows = 200;

        // when
        List<UUID> result = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            result.add(tested.registerCustomer("customer-" + i));
        }

        // then
        assertThat(result).doesNotHaveDuplicates();
        assertThat(result).allSatisfy(id -> assertIsUuidV7(id));
        assertThat(tested.countCustomers()).isEqualTo(rows);
    }

    @Test
    @DisplayName("IT update() when the entity is modified should keep the identifier assigned at insert time")
    void update_whenEntityIsModified_shouldKeepIdentifierAssignedAtInsertTime() {
        // given
        UUID id = tested.registerCustomer(CUSTOMER);

        // when
        tested.renameCustomer(id, "Ada Lovelace");

        // then
        Customer result = tested.findCustomer(id);
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Ada Lovelace");
        assertThat(result.getVersion()).isEqualTo(1L); // the row really was updated
        assertThat(tested.countCustomers()).isEqualTo(1);
    }

    @Test
    @DisplayName("IT persist() when children are cascaded should assign an identifier to each of them")
    void persist_whenChildrenAreCascaded_shouldAssignIdentifierToEachOfThem() {
        // given
        List<String> products = List.of("keyboard", "monitor", "desk");

        // when
        UUID customerId = tested.registerCustomerWithOrders(CUSTOMER, products);

        // then
        List<UUID> orderIds = tested.findOrderIds(customerId);
        assertThat(orderIds).hasSize(3).doesNotHaveDuplicates().doesNotContain(customerId);
        assertThat(orderIds).allSatisfy(id -> assertIsUuidV7(id));

        Customer reloaded = tested.findCustomer(customerId);
        assertThat(reloaded.getOrders()).extracting(Order::getProduct)
                .containsExactlyInAnyOrderElementsOf(products);
        assertThat(reloaded.getOrders()).allSatisfy(order ->
                assertThat(order.getCustomer().getId()).isEqualTo(customerId));
    }

    @Test
    @DisplayName("IT select() when rows are ordered by id should return them in insertion order")
    void select_whenRowsAreOrderedById_shouldReturnThemInInsertionOrder() throws InterruptedException {
        // given
        List<UUID> inserted = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            inserted.add(tested.registerCustomer("customer-" + i));
            Thread.sleep(2); // separate the inserts into distinct milliseconds
        }

        // when
        List<UUID> result = tested.findAllCustomerIdsSorted();

        // then
        assertThat(result).containsExactlyElementsOf(inserted);
    }

    @Test
    @DisplayName("IT persist() when called from several concurrent sessions should never collide on an identifier")
    void persist_whenCalledFromSeveralConcurrentSessions_shouldNeverCollideOnIdentifier() throws Exception {
        // given
        List<Callable<List<UUID>>> tasks = new ArrayList<>();
        for (int worker = 0; worker < THREADS; worker++) {
            int current = worker;
            tasks.add(() -> {
                List<UUID> ids = new ArrayList<>();
                for (int i = 0; i < PER_THREAD; i++) {
                    ids.add(tested.registerCustomer("worker-" + current + "-" + i));
                }
                return ids;
            });
        }

        // when
        List<UUID> result = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (Future<List<UUID>> future : pool.invokeAll(tasks)) {
                result.addAll(future.get());
            }
        }

        // then
        assertThat(result).hasSize(THREADS * PER_THREAD).doesNotHaveDuplicates();
        assertThat(tested.countCustomers()).isEqualTo(THREADS * PER_THREAD);
    }

    @Test
    @DisplayName("IT persist() when the annotation declares a generator should route the insert through it")
    void persist_whenAnnotationDeclaresGenerator_shouldRouteInsertThroughIt() {
        // when
        UUID result = tested.recordAudit("account created");

        // then
        assertThat(SequencedUuidGenerator.invocations()).isEqualTo(1);
        assertIsUuidV7(result);
        assertThat(timestampMillis(result)).isEqualTo(SequencedUuidGenerator.FIXED_INSTANT.toEpochMilli());
    }

    @Test
    @DisplayName("IT persist() when the annotation declares a generator should invoke it once per inserted row")
    void persist_whenAnnotationDeclaresGenerator_shouldInvokeItOncePerInsertedRow() {
        // when
        List<UUID> result = List.of(
                tested.recordAudit("first"),
                tested.recordAudit("second"),
                tested.recordAudit("third"));

        // then
        assertThat(SequencedUuidGenerator.invocations()).isEqualTo(3);
        assertThat(result).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("IT persist() when another entity declares a generator should leave the default one untouched")
    void persist_whenAnotherEntityDeclaresGenerator_shouldLeaveDefaultOneUntouched() {
        // when
        UUID auditId = tested.recordAudit("audited");
        UUID customerId = tested.registerCustomer(CUSTOMER);

        // then
        assertThat(timestampMillis(auditId)).isEqualTo(SequencedUuidGenerator.FIXED_INSTANT.toEpochMilli());
        assertThat(timestampMillis(customerId)).isGreaterThan(SequencedUuidGenerator.FIXED_INSTANT.toEpochMilli());
    }

    @Test
    @DisplayName("IT bootstrap when the declared generator cannot be instantiated should fail instead of the first insert")
    void bootstrap_whenDeclaredGeneratorCannotBeInstantiated_shouldFailInsteadOfFirstInsert() {
        // when
        Throwable result = catchThrowable(() -> HibernateSupport.buildSessionFactory(BrokenEntity.class));

        // then
        assertThat(result)
                .isNotNull()
                .hasStackTraceContaining(BrokenEntity.UninstantiableGenerator.class.getName())
                .hasStackTraceContaining("no-argument constructor")
                .rootCause().isInstanceOf(NoSuchMethodException.class);
    }
}
