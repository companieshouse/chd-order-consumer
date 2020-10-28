package uk.gov.companieshouse.chdorderconsumer.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.order.chd.MissingImageDeliveryRequestApi;
import uk.gov.companieshouse.chdorderconsumer.exception.RetryableErrorException;
import uk.gov.companieshouse.chdorderconsumer.exception.ServiceException;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;
import uk.gov.companieshouse.orders.items.Item;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class ItemOrderedProcessorService {

    private static final String POST_MISSING_IMAGE_CHD_ORDER_URI = "/chd-order-api/missing-image-deliveries";

    private final CHDOrderService chdOrderService;

    public ItemOrderedProcessorService(final CHDOrderService chdOrderService) {
        this.chdOrderService = chdOrderService;
    }

    MissingImageDeliveryRequestApi mapChdItemOrderedToMissingImageDeliveryRequestApi(ChdItemOrdered chdItemOrdered) {
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi = new MissingImageDeliveryRequestApi();
        Item item = chdItemOrdered.getItem();
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
        missingImageDeliveryRequestApi.setItemCost(item.getTotalItemCost());
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
        if (statusCode != BAD_REQUEST.value() && statusCode != UNAUTHORIZED.value()) {
            throw new RetryableErrorException(errorResponse);
        } else {
            throw new ServiceException(errorResponse);
        }
    }
}
