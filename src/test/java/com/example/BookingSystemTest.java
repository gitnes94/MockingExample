package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * Enhetstester för BookingSystem-klassen.
 * Testar alla metoder med både lyckade och misslyckade scenarios.
 * Använder Mockito för att mocka beroenden och AssertJ för assertions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingSystem Tests")
class BookingSystemTest {

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private NotificationService notificationService;

    private BookingSystem bookingSystem;

    private LocalDateTime currentTime;
    private LocalDateTime futureTime;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        bookingSystem = new BookingSystem(timeProvider, roomRepository, notificationService);
        currentTime = LocalDateTime.of(2024, 1, 15, 10, 0);
        futureTime = currentTime.plusHours(2);
        testRoom = new Room("room1", "Conference Room A");

        lenient().when(timeProvider.getCurrentTime()).thenReturn(currentTime);
    }

    @Nested
    @DisplayName("bookRoom() tests")
    class BookRoomTests {

        @Test
        @DisplayName("Ska lyckas boka ett ledigt rum")
        void shouldSuccessfullyBookAvailableRoom() throws NotificationException {
            // Arrange
            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.plusHours(1);

            when(roomRepository.findById("room1")).thenReturn(Optional.of(testRoom));

            // Act
            boolean result = bookingSystem.bookRoom("room1", startTime, endTime);

            // Assert
            assertThat(result).isTrue();
            verify(roomRepository).findById("room1");
            verify(roomRepository).save(testRoom);
            verify(notificationService).sendBookingConfirmation(any(Booking.class));
        }

        @Test
        @DisplayName("Ska misslyckas om rummet är upptaget")
        void shouldFailWhenRoomIsNotAvailable() throws NotificationException {
            // Arrange
            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.plusHours(1);

            // Lägg till en befintlig bokning
            Booking existingBooking = new Booking("booking1", "room1", startTime, endTime);
            testRoom.addBooking(existingBooking);

            when(roomRepository.findById("room1")).thenReturn(Optional.of(testRoom));

            // Act
            boolean result = bookingSystem.bookRoom("room1", startTime, endTime);

            // Assert
            assertThat(result).isFalse();
            verify(roomRepository).findById("room1");
            verify(roomRepository, never()).save(any(Room.class));
            verify(notificationService, never()).sendBookingConfirmation(any(Booking.class));
        }

        @Test
        @DisplayName("Ska kasta IllegalArgumentException om rummet inte existerar")
        void shouldThrowExceptionWhenRoomDoesNotExist() {
            // Arrange
            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.plusHours(1);

            when(roomRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> bookingSystem.bookRoom("nonexistent", startTime, endTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rummet existerar inte");

            verify(roomRepository).findById("nonexistent");
            verify(roomRepository, never()).save(any(Room.class));
        }

        @ParameterizedTest(name = "Ska kasta IllegalArgumentException när {0} är null")
        @MethodSource("provideNullParametersForBooking")
        @DisplayName("Ska kasta IllegalArgumentException för null-parametrar")
        void shouldThrowExceptionForNullParameters(String description, String roomId,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
            // Act & Assert
            assertThatThrownBy(() -> bookingSystem.bookRoom(roomId, startTime, endTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bokning kräver giltiga start- och sluttider samt rum-id");
        }

        private static Stream<Arguments> provideNullParametersForBooking() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime later = now.plusHours(1);

            return Stream.of(
                    Arguments.of("roomId", null, now, later),
                    Arguments.of("startTime", "room1", null, later),
                    Arguments.of("endTime", "room1", now, null),
                    Arguments.of("alla parametrar", null, null, null)
            );
        }

        @Test
        @DisplayName("Ska kasta IllegalArgumentException om starttid är i dåtid")
        void shouldThrowExceptionWhenStartTimeIsInPast() {
            // Arrange
            LocalDateTime pastTime = currentTime.minusHours(1);
            LocalDateTime endTime = currentTime.plusHours(1);

            // Act & Assert
            assertThatThrownBy(() -> bookingSystem.bookRoom("room1", pastTime, endTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Kan inte boka tid i dåtid");
        }

        @Test
        @DisplayName("Ska kasta IllegalArgumentException om sluttid är före starttid")
        void shouldThrowExceptionWhenEndTimeIsBeforeStartTime() {
            // Arrange
            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.minusHours(1);

            // act & assert
            assertThatThrownBy(() -> bookingSystem.bookRoom("room1", startTime, endTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sluttid måste vara efter starttid");
        }

        @Test
        @DisplayName("Ska fortsätta även om notifiering misslyckas")
        void shouldContinueEvenIfNotificationFails() throws NotificationException {
            // Arrange
            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.plusHours(1);

            when(roomRepository.findById("room1")).thenReturn(Optional.of(testRoom));
            doThrow(new NotificationException("Email server down"))
                    .when(notificationService).sendBookingConfirmation(any(Booking.class));

            // Act
            boolean result = bookingSystem.bookRoom("room1", startTime, endTime);

            // Assert
            assertThat(result).isTrue();
            verify(roomRepository).save(testRoom);
            verify(notificationService).sendBookingConfirmation(any(Booking.class));
        }

        @Test
        @DisplayName("Ska kunna boka rum vid nuvarande tidpunkt")
        void shouldBookRoomAtCurrentTime() {
            // Arrange
            LocalDateTime startTime = currentTime;
            LocalDateTime endTime = currentTime.plusHours(1);

            when(roomRepository.findById("room1")).thenReturn(Optional.of(testRoom));

            // Act
            boolean result = bookingSystem.bookRoom("room1", startTime, endTime);

            // Assert
            assertThat(result).isTrue();
            verify(roomRepository).save(testRoom);
        }

        @Test
        @DisplayName("Ska kunna boka rum som delvis överlappar en befintlig bokning")
        void shouldFailToBookRoomWithPartialOverlap() {
            // Arrange
            LocalDateTime existingStart = futureTime;
            LocalDateTime existingEnd = futureTime.plusHours(2);
            Booking existingBooking = new Booking("booking1", "room1", existingStart, existingEnd);
            testRoom.addBooking(existingBooking);

            // Försök boka med delvis överlapp
            LocalDateTime newStart = futureTime.plusHours(1);
            LocalDateTime newEnd = futureTime.plusHours(3);

            when(roomRepository.findById("room1")).thenReturn(Optional.of(testRoom));

            // Act
            boolean result = bookingSystem.bookRoom("room1", newStart, newEnd);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getAvailableRooms() tests")
    class GetAvailableRoomsTests {

        @Test
        @DisplayName("Ska returnera alla lediga rum")
        void shouldReturnAllAvailableRooms() {
            // Arrange
            Room room1 = new Room("room1", "Room 1");
            Room room2 = new Room("room2", "Room 2");
            Room room3 = new Room("room3", "Room 3");

            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.plusHours(1);

            when(roomRepository.findAll()).thenReturn(List.of(room1, room2, room3));

            // Act
            List<Room> availableRooms = bookingSystem.getAvailableRooms(startTime, endTime);

            // Assert
            assertThat(availableRooms)
                    .hasSize(3)
                    .containsExactlyInAnyOrder(room1, room2, room3);
            verify(roomRepository).findAll();
        }

        @Test
        @DisplayName("Ska exkludera upptagna rum")
        void shouldExcludeOccupiedRooms() {
            // Arrange
            Room room1 = new Room("room1", "Room 1");
            Room room2 = new Room("room2", "Room 2");
            Room room3 = new Room("room3", "Room 3");

            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.plusHours(1);

            // Boka rum2
            Booking booking = new Booking("booking1", "room2", startTime, endTime);
            room2.addBooking(booking);

            when(roomRepository.findAll()).thenReturn(List.of(room1, room2, room3));

            // Act
            List<Room> availableRooms = bookingSystem.getAvailableRooms(startTime, endTime);

            // Assert
            assertThat(availableRooms)
                    .hasSize(2)
                    .containsExactlyInAnyOrder(room1, room3)
                    .doesNotContain(room2);
        }

        @Test
        @DisplayName("Ska returnera tom lista om inga rum är lediga")
        void shouldReturnEmptyListWhenNoRoomsAvailable() {
            // Arrange
            Room room1 = new Room("room1", "Room 1");

            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.plusHours(1);

            Booking booking = new Booking("booking1", "room1", startTime, endTime);
            room1.addBooking(booking);

            when(roomRepository.findAll()).thenReturn(List.of(room1));

            // Act
            List<Room> availableRooms = bookingSystem.getAvailableRooms(startTime, endTime);

            // Assert
            assertThat(availableRooms).isEmpty();
        }

        @Test
        @DisplayName("Ska returnera tom lista om inga rum existerar")
        void shouldReturnEmptyListWhenNoRoomsExist() {
            // Arrange
            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.plusHours(1);

            when(roomRepository.findAll()).thenReturn(new ArrayList<>());

            // Act
            List<Room> availableRooms = bookingSystem.getAvailableRooms(startTime, endTime);

            // Assert
            assertThat(availableRooms).isEmpty();
        }

        @ParameterizedTest(name = "Ska kasta IllegalArgumentException när {0} är null")
        @MethodSource("provideNullParametersForAvailableRooms")
        @DisplayName("Ska kasta IllegalArgumentException för null-parametrar")
        void shouldThrowExceptionForNullParameters(String description,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
            // Act & Assert
            assertThatThrownBy(() -> bookingSystem.getAvailableRooms(startTime, endTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Måste ange både start- och sluttid");
        }

        private static Stream<Arguments> provideNullParametersForAvailableRooms() {
            LocalDateTime now = LocalDateTime.now();

            return Stream.of(
                    Arguments.of("startTime", null, now),
                    Arguments.of("endTime", now, null),
                    Arguments.of("båda parametrar", null, null)
            );
        }

        @Test
        @DisplayName("Ska kasta IllegalArgumentException om sluttid är före starttid")
        void shouldThrowExceptionWhenEndTimeIsBeforeStartTime() {
            // Arrange
            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.minusHours(1);

            // Act & Assert
            assertThatThrownBy(() -> bookingSystem.getAvailableRooms(startTime, endTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sluttid måste vara efter starttid");
        }

        @Test
        @DisplayName("Ska hantera rum med flera bokningar korrekt")
        void shouldHandleRoomsWithMultipleBookings() {
            // Arrange
            Room room1 = new Room("room1", "Room 1");
            Room room2 = new Room("room2", "Room 2");

            LocalDateTime searchStart = futureTime.plusHours(5);
            LocalDateTime searchEnd = futureTime.plusHours(6);

            // Lägg till flera bokningar i room1 (men inte under söktiden)
            Booking booking1 = new Booking("b1", "room1", futureTime, futureTime.plusHours(1));
            Booking booking2 = new Booking("b2", "room1", futureTime.plusHours(2), futureTime.plusHours(3));
            room1.addBooking(booking1);
            room1.addBooking(booking2);

            // Lägg till bokning i room2 som överlappar söktiden
            Booking booking3 = new Booking("b3", "room2", searchStart, searchEnd);
            room2.addBooking(booking3);

            when(roomRepository.findAll()).thenReturn(List.of(room1, room2));

            // Act
            List<Room> availableRooms = bookingSystem.getAvailableRooms(searchStart, searchEnd);

            // Assert
            assertThat(availableRooms)
                    .hasSize(1)
                    .containsExactly(room1);
        }
    }

    @Nested
    @DisplayName("cancelBooking() tests")
    class CancelBookingTests {

        @Test
        @DisplayName("Ska lyckas avboka en framtida bokning")
        void shouldSuccessfullyCancelFutureBooking() throws NotificationException {
            // Arrange
            LocalDateTime bookingStart = futureTime;
            LocalDateTime bookingEnd = futureTime.plusHours(1);
            Booking booking = new Booking("booking1", "room1", bookingStart, bookingEnd);
            testRoom.addBooking(booking);

            when(roomRepository.findAll()).thenReturn(List.of(testRoom));

            // Act
            boolean result = bookingSystem.cancelBooking("booking1");

            // Assert
            assertThat(result).isTrue();
            verify(roomRepository).findAll();
            verify(roomRepository).save(testRoom);
            verify(notificationService).sendCancellationConfirmation(booking);
        }

        @Test
        @DisplayName("Ska returnera false om bokningen inte hittas")
        void shouldReturnFalseWhenBookingNotFound() throws NotificationException {
            // Arrange
            when(roomRepository.findAll()).thenReturn(List.of(testRoom));

            // Act
            boolean result = bookingSystem.cancelBooking("nonexistent");

            // Assert
            assertThat(result).isFalse();
            verify(roomRepository).findAll();
            verify(roomRepository, never()).save(any(Room.class));
            verify(notificationService, never()).sendCancellationConfirmation(any(Booking.class));
        }

        @Test
        @DisplayName("Ska kasta IllegalArgumentException för null boknings-id")
        void shouldThrowExceptionForNullBookingId() {
            // Act & Assert
            assertThatThrownBy(() -> bookingSystem.cancelBooking(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Boknings-id kan inte vara null");

            verify(roomRepository, never()).findAll();
        }

        @Test
        @DisplayName("Ska kasta IllegalStateException om bokning redan har börjat")
        void shouldThrowExceptionWhenBookingHasStarted() {
            // Arrange
            LocalDateTime bookingStart = currentTime.minusHours(1);
            LocalDateTime bookingEnd = currentTime.plusHours(1);
            Booking booking = new Booking("booking1", "room1", bookingStart, bookingEnd);
            testRoom.addBooking(booking);

            when(roomRepository.findAll()).thenReturn(List.of(testRoom));

            // Act & Assert
            assertThatThrownBy(() -> bookingSystem.cancelBooking("booking1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Kan inte avboka påbörjad eller avslutad bokning");

            verify(roomRepository).findAll();
            verify(roomRepository, never()).save(any(Room.class));
        }

        @Test
        @DisplayName("Ska kasta IllegalStateException om bokning redan är avslutad")
        void shouldThrowExceptionWhenBookingHasEnded() {
            // Arrange
            LocalDateTime bookingStart = currentTime.minusHours(2);
            LocalDateTime bookingEnd = currentTime.minusHours(1);
            Booking booking = new Booking("booking1", "room1", bookingStart, bookingEnd);
            testRoom.addBooking(booking);

            when(roomRepository.findAll()).thenReturn(List.of(testRoom));

            // Act & Assert
            assertThatThrownBy(() -> bookingSystem.cancelBooking("booking1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Kan inte avboka påbörjad eller avslutad bokning");
        }

        @Test
        @DisplayName("Ska fortsätta även om notifiering misslyckas")
        void shouldContinueEvenIfNotificationFails() throws NotificationException {
            // Arrange
            LocalDateTime bookingStart = futureTime;
            LocalDateTime bookingEnd = futureTime.plusHours(1);
            Booking booking = new Booking("booking1", "room1", bookingStart, bookingEnd);
            testRoom.addBooking(booking);

            when(roomRepository.findAll()).thenReturn(List.of(testRoom));
            doThrow(new NotificationException("Email server down"))
                    .when(notificationService).sendCancellationConfirmation(any(Booking.class));

            // Act
            boolean result = bookingSystem.cancelBooking("booking1");

            // Assert
            assertThat(result).isTrue();
            verify(roomRepository).save(testRoom);
            verify(notificationService).sendCancellationConfirmation(booking);
        }

        @Test
        @DisplayName("Ska kunna hitta bokning i andra rummet")
        void shouldFindBookingInSecondRoom() {
            // Arrange
            Room room1 = new Room("room1", "Room 1");
            Room room2 = new Room("room2", "Room 2");

            LocalDateTime bookingStart = futureTime;
            LocalDateTime bookingEnd = futureTime.plusHours(1);
            Booking booking = new Booking("booking1", "room2", bookingStart, bookingEnd);
            room2.addBooking(booking);

            when(roomRepository.findAll()).thenReturn(List.of(room1, room2));

            // Act
            boolean result = bookingSystem.cancelBooking("booking1");

            // Assert
            assertThat(result).isTrue();
            verify(roomRepository).save(room2);
        }

        @Test
        @DisplayName("Ska kunna avboka vid gränstiden (exakt vid nuvarande tid)")
        void shouldCancelBookingAtBoundaryTime() throws NotificationException {
            // Arrange
            LocalDateTime bookingStart = currentTime.plusMinutes(1); // Strax i framtiden
            LocalDateTime bookingEnd = currentTime.plusHours(1);
            Booking booking = new Booking("booking1", "room1", bookingStart, bookingEnd);
            testRoom.addBooking(booking);

            when(roomRepository.findAll()).thenReturn(List.of(testRoom));

            // Act
            boolean result = bookingSystem.cancelBooking("booking1");

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Integration och edge case tests")
    class IntegrationAndEdgeCaseTests {

        @Test
        @DisplayName("Ska hantera flera rum med olika bokningar korrekt")
        void shouldHandleMultipleRoomsWithDifferentBookings() {
            // Arrange
            Room room1 = new Room("room1", "Room 1");
            Room room2 = new Room("room2", "Room 2");
            Room room3 = new Room("room3", "Room 3");

            LocalDateTime start1 = futureTime;
            LocalDateTime end1 = futureTime.plusHours(1);

            Booking booking1 = new Booking("b1", "room1", start1, end1);
            Booking booking2 = new Booking("b2", "room2", futureTime.plusHours(2), futureTime.plusHours(3));

            room1.addBooking(booking1);
            room2.addBooking(booking2);

            when(roomRepository.findAll()).thenReturn(List.of(room1, room2, room3));

            // Act
            List<Room> availableRooms = bookingSystem.getAvailableRooms(start1, end1);

            // Assert
            assertThat(availableRooms)
                    .hasSize(2)
                    .containsExactlyInAnyOrder(room2, room3);
        }

        @Test
        @DisplayName("Ska verifiera att rätt room sparas vid bokning")
        void shouldVerifyCorrectRoomIsSaved() throws NotificationException {
            // Arrange
            LocalDateTime startTime = futureTime;
            LocalDateTime endTime = futureTime.plusHours(1);

            when(roomRepository.findById("room1")).thenReturn(Optional.of(testRoom));

            // Act
            bookingSystem.bookRoom("room1", startTime, endTime);

            // Assert
            verify(roomRepository).save(testRoom);
        }

        @Test
        @DisplayName("Ska hantera samma starttid och sluttid (0 minuters bokning)")
        void shouldHandleZeroDurationBooking() throws NotificationException {
            // Arrange
            LocalDateTime time = futureTime;

            when(roomRepository.findById("room1")).thenReturn(Optional.of(testRoom));

            // Act
            boolean result = bookingSystem.bookRoom("room1", time, time);

            // Assert
            assertThat(result).isTrue();
        }
    }
}