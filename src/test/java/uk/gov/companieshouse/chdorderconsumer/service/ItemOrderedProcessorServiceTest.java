package uk.gov.companieshouse.chdorderconsumer.service;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.order.chd.MissingImageDeliveryRequestApi;
import uk.gov.companieshouse.chdorderconsumer.exception.RetryableErrorException;
import uk.gov.companieshouse.chdorderconsumer.exception.ServiceException;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;
import uk.gov.companieshouse.orders.items.Item;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(MockitoExtension.class)
class ItemOrderedProcessorServiceTest {
    private static final ChdItemOrdered CHD_ITEM_ORDERED;

    @Mock
    private CHDOrderService chdOrderService;

    @InjectMocks
    private ItemOrderedProcessorService processorUnderTest;

    static {
        Item item = new Item();
        item.setId("1");
        item.setCompanyName("Company Name");
        item.setCompanyNumber("00000000");
        item.setTotalItemCost("5");

        Map<String, String> itemOptions = new HashMap<>();
        itemOptions.put("filingHistoryCategory", "RESOLUTIONS");
        itemOptions.put("filingHistoryDate", "2009-04-03");
        itemOptions.put("filingHistoryDescription", "description");
        itemOptions.put("filingHistoryType", "Resolution");
        item.setItemOptions(itemOptions);

        CHD_ITEM_ORDERED = new ChdItemOrdered();
        CHD_ITEM_ORDERED.setOrderedAt("2020-10-27T09:39:10.873");
        CHD_ITEM_ORDERED.setPaymentReference("payment ref");
        CHD_ITEM_ORDERED.setItem(item);
    }

    @Test
    void mapChdItemOrderedToMissingImageDeliveryRequestApiCorrectly() {
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi
                = processorUnderTest.mapChdItemOrderedToMissingImageDeliveryRequestApi(CHD_ITEM_ORDERED);

        assertThat(missingImageDeliveryRequestApi.getId(), is(CHD_ITEM_ORDERED.getItem().getId()));
        assertThat(missingImageDeliveryRequestApi.getCompanyName(), is(CHD_ITEM_ORDERED.getItem().getCompanyName()));
        assertThat(missingImageDeliveryRequestApi.getCompanyNumber(), is(CHD_ITEM_ORDERED.getItem().getCompanyNumber()));
        assertThat(missingImageDeliveryRequestApi.getOrderedAt().toString(), is(CHD_ITEM_ORDERED.getOrderedAt()));
        assertThat(missingImageDeliveryRequestApi.getPaymentReference(), is(CHD_ITEM_ORDERED.getPaymentReference()));
        assertThat(missingImageDeliveryRequestApi.getFilingHistoryCategory(), is(CHD_ITEM_ORDERED.getItem().getItemOptions().get("filingHistoryCategory")));
        assertThat(missingImageDeliveryRequestApi.getFilingHistoryDate(), is(CHD_ITEM_ORDERED.getItem().getItemOptions().get("filingHistoryDate")));
        assertThat(missingImageDeliveryRequestApi.getFilingHistoryDescription(), is(CHD_ITEM_ORDERED.getItem().getItemOptions().get("filingHistoryDescription")));
        assertThat(missingImageDeliveryRequestApi.getFilingHistoryType(), is(CHD_ITEM_ORDERED.getItem().getItemOptions().get("filingHistoryType")));
        assertThat(missingImageDeliveryRequestApi.getItemCost(), is(CHD_ITEM_ORDERED.getItem().getTotalItemCost()));
    }

    @Test
    void propogatesRetryableExceptionIfApiErrorResponseExceptionIsInternalServerError() throws ApiErrorResponseException {
        when(chdOrderService.createCHDOrder(anyString(), any(MissingImageDeliveryRequestApi.class)))
                .thenThrow(buildApiErrorResponseException(INTERNAL_SERVER_ERROR));

        assertThrows(RetryableErrorException.class, () ->
                processorUnderTest.processItemOrdered(CHD_ITEM_ORDERED));
    }

    @Test
    void propogatesServiceExceptionIfApiErrorResponseExceptionIsBadRequest() throws ApiErrorResponseException {
        when(chdOrderService.createCHDOrder(anyString(), any(MissingImageDeliveryRequestApi.class)))
                .thenThrow(buildApiErrorResponseException(BAD_REQUEST));

        assertThrows(ServiceException.class, () ->
                processorUnderTest.processItemOrdered(CHD_ITEM_ORDERED));
    }

    @Test
    void propogatesServiceExceptionIfApiErrorResponseExceptionIsUnauthorised() throws ApiErrorResponseException {
        when(chdOrderService.createCHDOrder(anyString(), any(MissingImageDeliveryRequestApi.class)))
                .thenThrow(buildApiErrorResponseException(UNAUTHORIZED));

        assertThrows(ServiceException.class, () ->
                processorUnderTest.processItemOrdered(CHD_ITEM_ORDERED));
    }

    @Test
    void propogatesRetryableExceptionIfApiResponseIsRequestTimeOut() throws ApiErrorResponseException {
        ApiResponse<MissingImageDeliveryRequestApi> apiResponse =
                new ApiResponse<>(REQUEST_TIMEOUT.value(), new HttpHeaders(),
                        new MissingImageDeliveryRequestApi());

        when(chdOrderService.createCHDOrder(anyString(), any(MissingImageDeliveryRequestApi.class)))
                .thenReturn(apiResponse);

        assertThrows(RetryableErrorException.class, () ->
                processorUnderTest.processItemOrdered(CHD_ITEM_ORDERED));
    }

    @Test
    void propogatesServiceExceptionIfApiResponseIsBadRequest() throws ApiErrorResponseException {
        ApiResponse<MissingImageDeliveryRequestApi> apiResponse =
                new ApiResponse<>(BAD_REQUEST.value(), new HttpHeaders(),
                        new MissingImageDeliveryRequestApi());

        when(chdOrderService.createCHDOrder(anyString(), any(MissingImageDeliveryRequestApi.class)))
                .thenReturn(apiResponse);

        assertThrows(ServiceException.class, () ->
                processorUnderTest.processItemOrdered(CHD_ITEM_ORDERED));
    }

    @Test
    void propogatesServiceExceptionIfApiResponseIsUnauthorised() throws ApiErrorResponseException {
        ApiResponse<MissingImageDeliveryRequestApi> apiResponse =
                new ApiResponse<>(UNAUTHORIZED.value(), new HttpHeaders(),
                        new MissingImageDeliveryRequestApi());

        when(chdOrderService.createCHDOrder(anyString(), any(MissingImageDeliveryRequestApi.class)))
                .thenReturn(apiResponse);

        assertThrows(ServiceException.class, () ->
                processorUnderTest.processItemOrdered(CHD_ITEM_ORDERED));
    }

    private ApiErrorResponseException buildApiErrorResponseException(HttpStatus httpStatus) {
        return new ApiErrorResponseException(
                new HttpResponseException.Builder(httpStatus.value(), "Message", new HttpHeaders()));
    }
}
