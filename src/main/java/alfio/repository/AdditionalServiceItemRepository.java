/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.repository;

import alfio.model.AdditionalServiceItem;
import alfio.model.AdditionalServiceItem.AdditionalServiceItemStatus;
import alfio.model.AdditionalServiceItemExport;
import alfio.model.BookedAdditionalService;
import ch.digitalfondue.npjt.*;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

@QueryRepository
public interface AdditionalServiceItemRepository {

    String SELECT_BOOKED_ADDITIONAL_SERVICES = "select asd.value as as_name, ads.id as_id, count(ads.id) as qty from additional_service_item ai" +
        "  join additional_service ads on additional_service_id_fk = ads.id" +
        "  join additional_service_description asd on ads.id = asd.additional_service_id_fk" +
        "  where ai.event_id_fk = :eventId and ai.status in ('ACQUIRED', 'CHECKED_IN', 'TO_BE_PAID')" +
        "  and ads.service_type <> 'DONATION'" +
        "  and ads.supplement_policy <> 'MANDATORY_ONE_FOR_TICKET'" +
        "  and asd.locale = :language" +
        "  and asd.type = 'TITLE'" +
        "  and ai.tickets_reservation_uuid = :reservationId";
    String UPDATE_STATUS = "update additional_service_item set status = :status where event_id_fk = :eventId and tickets_reservation_uuid = :reservationUuid";
    String FIND_BY_RESERVATION_ID = "select * from additional_service_item where event_id_fk = :eventId and tickets_reservation_uuid = :reservationUuid";

    @Query("insert into additional_service_item (uuid, creation, tickets_reservation_uuid, additional_service_id_fk, status, event_id_fk, src_price_cts, final_price_cts, vat_cts, discount_cts, currency_code) " +
        "values(:uuid, :creation, :ticketsReservationUuid, :additionalServiceId, :status, :eventId, :srcPriceCts, :finalPriceCts, :vatCts, :discountCts, :currencyCode)")
    @AutoGeneratedKey("id")
    AffectedRowCountAndKey<Integer> insert(@Bind("uuid") String uuid,
                                           @Bind("creation") ZonedDateTime creation,
                                           @Bind("ticketsReservationUuid") String ticketsReservationUuid,
                                           @Bind("additionalServiceId") int additionalServiceId,
                                           @Bind("status") AdditionalServiceItemStatus status,
                                           @Bind("eventId") int eventId,
                                           @Bind("srcPriceCts") Integer srcPriceCts,
                                           @Bind("finalPriceCts") Integer finalPriceCts,
                                           @Bind("vatCts") Integer vatCts,
                                           @Bind("discountCts") Integer discountCts,
                                           @Bind("currencyCode") String currencyCode);

    @Query(FIND_BY_RESERVATION_ID)
    List<AdditionalServiceItem> findByReservationUuid(@Bind("eventId") int eventId, @Bind("reservationUuid") String reservationUuid);

    @Query(FIND_BY_RESERVATION_ID + " and ticket_id_fk = :ticketId")
    List<AdditionalServiceItem> findByTicketId(@Bind("eventId") int eventId,
                                               @Bind("reservationUuid") String reservationUuid,
                                               @Bind("ticketId") int ticketId);

    @Query(UPDATE_STATUS)
    int updateItemsStatusWithReservationUUID(@Bind("eventId") int eventId, @Bind("reservationUuid") String reservationUuid, @Bind("status") AdditionalServiceItemStatus status);

    @Query(UPDATE_STATUS + " and ticket_id_fk = :ticketId")
    int updateItemsStatusWithTicketId(@Bind("eventId") int eventId, @Bind("reservationUuid") String reservationUuid, @Bind("ticketId") int ticketId, @Bind("status") AdditionalServiceItemStatus status);

    @Query("select count(*) > 0 from additional_service_item " +
        " inner join additional_service on additional_service_id_fk = additional_service.id " +
        " where additional_service_item.event_id_fk = :eventId and service_type = 'SUPPLEMENT' and tickets_reservation_uuid = :reservationId and final_price_cts > 0")
    boolean hasPaidSupplements(@Bind("eventId") int eventId, @Bind("reservationId") String reservationId);

    @Query("select count(*) from additional_service_item where event_id_fk = :eventId and tickets_reservation_uuid = :reservationId")
    int countByReservationUuid(@Bind("eventId") int eventId, @Bind("reservationId") String reservationId);

    @Query(SELECT_BOOKED_ADDITIONAL_SERVICES + "  group by ads.id, asd.value")
    List<BookedAdditionalService> getAdditionalServicesBookedForReservation(@Bind("reservationId") String reservationId,
                                                                            @Bind("language") String language,
                                                                            @Bind("eventId") int eventId);

    @Query(SELECT_BOOKED_ADDITIONAL_SERVICES +
        "  and ai.ticket_id_fk = :ticketId " +
        "  group by ads.id, asd.value")
    List<BookedAdditionalService> getAdditionalServicesBookedForTicket(@Bind("reservationId") String reservationId,
                                                                       @Bind("ticketId") int ticketId,
                                                                       @Bind("language") String language,
                                                                       @Bind("eventId") int eventId);


    @Query(
        "select" +
            "    ai.uuid ai_uuid, ai.creation ai_creation, ai.last_modified ai_last_modified, ai.final_price_cts ai_final_price_cts, ai.currency_code ai_currency_code, ai.vat_cts ai_vat_cts, ai.discount_cts ai_discount_cts," +
            "    tr.id tr_uuid, tr.first_name tr_first_name, tr.last_name tr_last_name, tr.email_address tr_email_address," +
            "    asv.service_type as_type, asd.value as_title, ai.status as ai_status " +
            " from additional_service_item ai" +
            "    join additional_service asv on ai.additional_service_id_fk = asv.id" +
            "    join tickets_reservation tr on ai.tickets_reservation_uuid = tr.id" +
            "    join additional_service_description asd on ai.additional_service_id_fk = asd.additional_service_id_fk" +
            " where ai.event_id_fk = :eventId" +
            "  and asv.service_type = :additionalServiceType" +
            "  and asd.type = 'TITLE'" +
            "  and asd.locale = :locale " +
            "  and ai.status in ('ACQUIRED', 'CHECKED_IN', 'TO_BE_PAID') "
    )
    List<AdditionalServiceItemExport> getAdditionalServicesOfTypeForEvent(@Bind("eventId") int eventId,
                                                                          @Bind("additionalServiceType") String additionalServiceType,
                                                                          @Bind("locale") String locale);

    @Query("select count(*) from additional_service_item where additional_service_id_fk = :serviceId and status <> 'INVALIDATED'")
    int countItemsForService(@Bind("serviceId") int additionalServiceId);

    @Query(type = QueryType.TEMPLATE, value = "insert into additional_service_item (uuid, creation, tickets_reservation_uuid, additional_service_id_fk, status, event_id_fk, src_price_cts, final_price_cts, vat_cts, discount_cts, currency_code) " +
        "values(:uuid, now(), :ticketsReservationUuid, :additionalServiceId, :status, :eventId, :srcPriceCts, :finalPriceCts, :vatCts, :discountCts, :currencyCode)")
    String batchInsert();

    @Query(type = QueryType.TEMPLATE, value = "update additional_service_item set tickets_reservation_uuid = :ticketsReservationUuid, status = :status, src_price_cts = :srcPriceCts, final_price_cts = :finalPriceCts, vat_cts = :vatCts, discount_cts = :discountCts, currency_code = :currencyCode" +
        " where id = :id and additional_service_id_fk = :additionalServiceId")
    String batchUpdate();

    @Query("update additional_service_item set status = 'INVALIDATED' where id in (select id from additional_service_item where additional_service_id_fk = :serviceId and status = 'FREE' limit :count)")
    int invalidateItems(@Bind("serviceId") int additionalServiceId, @Bind("count") int count);

    @Query("select id from additional_service_item where additional_service_id_fk = :serviceId and status = 'FREE' limit :count for update skip locked")
    List<Integer> lockExistingItems(@Bind("serviceId") int additionalServiceId, @Bind("count") int count);

    @Query(type = QueryType.TEMPLATE, value = "update additional_service_item set ticket_id_fk = :ticketId" +
        " where id = :itemId and tickets_reservation_uuid = :reservationId and status = 'PENDING'")
    String batchLinkToTicket();

    @Query("delete from additional_service_item asi" +
        " using additional_service adds" +
        " where adds.id = asi.additional_service_id_fk and asi.event_id_fk = :eventId and asi.tickets_reservation_uuid = :reservationId" +
        " and adds.available_qty = -1")
    int deleteAdditionalServiceItemsByReservationId(@Bind("eventId") int eventId, @Bind("reservationId") String reservationId);

    @Query("update additional_service_item asi set ticket_id_fk = null, status = 'FREE', tickets_reservation_uuid = null" +
        " from additional_service adds where adds.id = asi.additional_service_id_fk" +
        " and asi.event_id_fk = :eventId and asi.tickets_reservation_uuid = :reservationId" +
        " and adds.available_qty > 0")
    int revertAdditionalServiceItemsByReservationId(@Bind("eventId") int eventId, @Bind("reservationId") String reservationId);

    @Query("select count(*) from additional_service_item asi" +
        " join ticket t on asi.ticket_id_fk = t.id and asi.event_id_fk = t.event_id" +
        " where t.uuid = :ticketUUID and asi.id in (:additionalServiceIds)")
    int countMatchingItemsForTicket(@Bind("ticketUUID") String ticketUuid, @Bind("additionalServiceIds") Collection<Integer> ids);
}
