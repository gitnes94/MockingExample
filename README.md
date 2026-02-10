# Laboration 2: Unit Testing

## Om projektet

Laboration i enhetstestning med JUnit 5, Mockito och Test-Driven Development (TDD).


## Uppgifter

### Uppgift 1: BookingSystem

Enhetstester för ett bokningssystem med mockade beroenden.

- 32 tester
- Använder Mockito för TimeProvider, RoomRepository och NotificationService
- Coverage: >90%

### Uppgift 1 (VG): PaymentProcessor

Refaktorering av PaymentProcessor för testbarhet.

- 41 tester
- Extraherade interfaces: PaymentGateway, PaymentRepository, EmailNotificationService
- Coverage: >95%

### Uppgift 2: ShoppingCart

TDD-implementation av en shoppingcart.

- 16 tester
- Följer Red-Green-Refactor
- Stöder: lägg till varor, ta bort varor, kvantitet, totalpris, rabatter

## Köra tester

```bash
mvn clean test
```

## Testresultat

```
Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
```


## Mutation testing

```
================================================================================
- Statistics
================================================================================
>> Line Coverage (for mutated classes only): 167/184 (91%)
>> 24 tests examined
>> Generated 124 mutations Killed 97 (78%)
>> Mutations with no coverage 16. Test strength 90%
>> Ran 207 tests (1.67 tests per mutation)

```
