package uk.gov.companieshouse.chdorderconsumer.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.order.chd.MissingImageDeliveryRequestApi;
import uk.gov.companieshouse.chdorderconsumer.exception.DuplicateErrorException;
import uk.gov.companieshouse.chdorderconsumer.exception.RetryableErrorException;
import uk.gov.companieshouse.chdorderconsumer.exception.ServiceException;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;
import uk.gov.companieshouse.orders.items.Item;
import uk.gov.companieshouse.orders.items.OrderedBy;

@Service
public class ItemOrderedProcessorService {

    private static final String POST_MISSING_IMAGE_CHD_ORDER_URI = "/chd-order-api/missing-image-deliveries";

    private final CHDOrderService chdOrderService;

    private final MongoService mongoService;

    public ItemOrderedProcessorService(final CHDOrderService chdOrderService,
                                       final MongoService mongoService) {
        this.chdOrderService = chdOrderService;
        this.mongoService = mongoService;
    }

    MissingImageDeliveryRequestApi mapChdItemOrderedToMissingImageDeliveryRequestApi(ChdItemOrdered chdItemOrdered) {
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi = new MissingImageDeliveryRequestApi();
        Item item = chdItemOrdered.getItem();
        OrderedBy orderedBy = chdItemOrdered.getOrderedBy();
        Map<String, String> itemOptions = item.getItemOptions();

        missingImageDeliveryRequestApi.setId(item.getId());
        missingImageDeliveryRequestApi.setCompanyName(item.getCompanyName());
        missingImageDeliveryRequestApi.setCompanyNumber(item.getCompanyNumber());
        missingImageDeliveryRequestApi.setOrderedAt(LocalDateTime.parse(chdItemOrdered.getOrderedAt()));
        missingImageDeliveryRequestApi.setPaymentReference(chdItemOrdered.getPaymentReference());
        missingImageDeliveryRequestApi.setFilingHistoryCategory(itemOptions.get("filingHistoryCategory"));
        missingImageDeliveryRequestApi.setFilingHistoryDate(itemOptions.get("filingHistoryDate"));
        missingImageDeliveryRequestApi.setFilingHistoryDescription(itemOptions.get("filingHistoryDescription"));
        missingImageDeliveryRequestApi.setFilingHistoryType(itemOptions.get("filingHistoryType"));
        missingImageDeliveryRequestApi.setFilingHistoryBarcode(itemOptions.get("filingHistoryBarcode"));
        missingImageDeliveryRequestApi.setItemCost(item.getTotalItemCost());
        missingImageDeliveryRequestApi.setEmailAddress(orderedBy.getEmail());

        String filingHistoryId = itemOptions.get("filingHistoryId");

        missingImageDeliveryRequestApi.setEntityId(mongoService.getEntityId(filingHistoryId));

        if (missingImageDeliveryRequestApi.getEntityId() == null &&
            missingImageDeliveryRequestApi.getFilingHistoryBarcode() == null) {

            missingImageDeliveryRequestApi.setFilingHistoryBarcode(mongoService.getBarcode(filingHistoryId));
        }

        return missingImageDeliveryRequestApi;
    }

    public void processItemOrdered(ChdItemOrdered chdItemOrdered) {
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi = new MissingImageDeliveryRequestApi();
        try {
            missingImageDeliveryRequestApi = mapChdItemOrderedToMissingImageDeliveryRequestApi(chdItemOrdered);

            ApiResponse<MissingImageDeliveryRequestApi> missingImageDeliveryRequestApiResponse =
                    chdOrderService.createCHDOrder(POST_MISSING_IMAGE_CHD_ORDER_URI, missingImageDeliveryRequestApi);
            if (missingImageDeliveryRequestApiResponse.getStatusCode() != CREATED.value()) {
                processError(missingImageDeliveryRequestApiResponse.getStatusCode(), missingImageDeliveryRequestApiResponse.toString());
            }
        } catch (ApiErrorResponseException ex) {
            String id = missingImageDeliveryRequestApi.getId() != null ? missingImageDeliveryRequestApi.getId() : "Unknown";
            String errorResponse = "API Response Error for : "
                    + id + ", Error response: " + ex.toString();
            processError(ex.getStatusCode(), errorResponse);
        }
    }

    private void processError(int statusCode, String errorResponse) {
        if (statusCode != BAD_REQUEST.value() && statusCode != UNAUTHORIZED.value() && statusCode != CONFLICT.value()) {
            throw new RetryableErrorException(errorResponse);
        } else if( statusCode == CONFLICT.value() ){
            throw new DuplicateErrorException(errorResponse);
        } else {
            throw new ServiceException(errorResponse);
        }
    }
}
