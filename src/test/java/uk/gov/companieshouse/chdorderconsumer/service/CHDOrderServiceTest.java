package uk.gov.companieshouse.chdorderconsumer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.order.chd.PrivateChdOrderResourceHandler;
import uk.gov.companieshouse.api.handler.order.chd.request.ChdOrdersPost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.order.chd.MissingImageDeliveryRequestApi;
import uk.gov.companieshouse.chdorderconsumer.exception.ServiceException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CHDOrderServiceTest {

    @InjectMocks
    private CHDOrderService chdOrderService;

    @Mock
    private ApiClientService mockApiClientService;

    @Mock
    private InternalApiClient mockApiClient;

    @Mock
    private PrivateChdOrderResourceHandler mockPrivateChdOrderResourceHandler;

    @Mock
    private ChdOrdersPost mockChdOrdersPost;

    @Mock
    private ApiResponse<MissingImageDeliveryRequestApi> mockApiResponse;

    private static final String POST_MISSING_IMAGE_CHD_ORDER_URI = "/chd-order-api/missing-image-deliveries";
    private static final String POST_MISSING_IMAGE_CHD_ORDER_INCORRECT_URI = "/chd-order-api/missing-image-delivery";
    private static final MissingImageDeliveryRequestApi MISSING_IMAGE_DELIVERY_REQUEST_API;
    static {
        MISSING_IMAGE_DELIVERY_REQUEST_API = new MissingImageDeliveryRequestApi();
        MISSING_IMAGE_DELIVERY_REQUEST_API.setCompanyName("Test");
        MISSING_IMAGE_DELIVERY_REQUEST_API.setCompanyNumber("123");
        MISSING_IMAGE_DELIVERY_REQUEST_API.setFilingHistoryType("TestType");
        MISSING_IMAGE_DELIVERY_REQUEST_API.setFilingHistoryCategory("Test");
        MISSING_IMAGE_DELIVERY_REQUEST_API.setFilingHistoryDate("25-10-2018");
        MISSING_IMAGE_DELIVERY_REQUEST_API.setFilingHistoryDescription("Test");
        MISSING_IMAGE_DELIVERY_REQUEST_API.setId("Test");
        MISSING_IMAGE_DELIVERY_REQUEST_API.setItemCost("Test");
        MISSING_IMAGE_DELIVERY_REQUEST_API.setOrderedAt(LocalDateTime.now());
        MISSING_IMAGE_DELIVERY_REQUEST_API.setPaymentReference("Test");
    }

    @BeforeEach
    void init() {
        when(mockApiClientService.getInternalApiClient()).thenReturn(mockApiClient);
        when(mockApiClient.privateChdOrderResourceHandler()).thenReturn(mockPrivateChdOrderResourceHandler);
    }

    @Test
    void createCHDOrderSuccessful() throws ApiErrorResponseException, URIValidationException {
        when(mockPrivateChdOrderResourceHandler
            .postChdOrder(POST_MISSING_IMAGE_CHD_ORDER_URI, MISSING_IMAGE_DELIVERY_REQUEST_API))
            .thenReturn(mockChdOrdersPost);
        when(mockChdOrdersPost.execute()).thenReturn(mockApiResponse);

        ApiResponse<MissingImageDeliveryRequestApi> response
            = chdOrderService.createCHDOrder(POST_MISSING_IMAGE_CHD_ORDER_URI, MISSING_IMAGE_DELIVERY_REQUEST_API);

        assertEquals(mockApiResponse, response);
    }

    @Test
    void createCHDOrderThrowsUriValidationException() throws ApiErrorResponseException, URIValidationException {
        when(mockPrivateChdOrderResourceHandler
            .postChdOrder(POST_MISSING_IMAGE_CHD_ORDER_INCORRECT_URI, MISSING_IMAGE_DELIVERY_REQUEST_API))
            .thenReturn(mockChdOrdersPost);
        when(mockChdOrdersPost.execute()).thenThrow(new URIValidationException("Test exception"));

        ServiceException exception =
            assertThrows(ServiceException.class, () -> chdOrderService.createCHDOrder(
                POST_MISSING_IMAGE_CHD_ORDER_INCORRECT_URI, MISSING_IMAGE_DELIVERY_REQUEST_API));
        assertEquals("Unrecognised uri pattern for: " + POST_MISSING_IMAGE_CHD_ORDER_INCORRECT_URI,
            exception.getMessage());
    }

    @Test
    void createCHDOrderThrowsApiResponseException() throws ApiErrorResponseException, URIValidationException {
        when(mockPrivateChdOrderResourceHandler
            .postChdOrder(POST_MISSING_IMAGE_CHD_ORDER_URI, MISSING_IMAGE_DELIVERY_REQUEST_API))
            .thenReturn(mockChdOrdersPost);
        when(mockChdOrdersPost.execute()).thenThrow(ApiErrorResponseException.class);

        assertThrows(ApiErrorResponseException.class, () -> chdOrderService.createCHDOrder(
            POST_MISSING_IMAGE_CHD_ORDER_URI, MISSING_IMAGE_DELIVERY_REQUEST_API));
    }
}
