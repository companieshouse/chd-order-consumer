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
    private static final LocalDateTime DEFAULT_DATE = LocalDateTime.of(2019, 1, 1, 0, 0, 0);

    @BeforeEach
    void init() {
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi = createMissingImageDeliveryRequestApi();

        when(mockApiClientService.getInternalApiClient()).thenReturn(mockApiClient);
        when(mockApiClient.privateChdOrderResourceHandler()).thenReturn(mockPrivateChdOrderResourceHandler);
    }

    @Test
    void createCHDOrderSuccessful() throws ApiErrorResponseException, URIValidationException {
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi = createMissingImageDeliveryRequestApi();

        when(mockPrivateChdOrderResourceHandler
            .postChdOrder(POST_MISSING_IMAGE_CHD_ORDER_URI, missingImageDeliveryRequestApi))
            .thenReturn(mockChdOrdersPost);
        when(mockChdOrdersPost.execute()).thenReturn(mockApiResponse);

        ApiResponse<MissingImageDeliveryRequestApi> response
            = chdOrderService.createCHDOrder(POST_MISSING_IMAGE_CHD_ORDER_URI, missingImageDeliveryRequestApi);

        assertEquals(mockApiResponse, response);
    }

    @Test
    void createCHDOrderThrowsUriValidationException() throws ApiErrorResponseException, URIValidationException {
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi = createMissingImageDeliveryRequestApi();

        when(mockPrivateChdOrderResourceHandler
            .postChdOrder(POST_MISSING_IMAGE_CHD_ORDER_INCORRECT_URI, missingImageDeliveryRequestApi))
            .thenReturn(mockChdOrdersPost);
        when(mockChdOrdersPost.execute()).thenThrow(new URIValidationException("Test exception"));

        ServiceException exception =
            assertThrows(ServiceException.class, () -> chdOrderService.createCHDOrder(
                POST_MISSING_IMAGE_CHD_ORDER_INCORRECT_URI, missingImageDeliveryRequestApi));
        assertEquals("Unrecognised uri pattern for: " + POST_MISSING_IMAGE_CHD_ORDER_INCORRECT_URI,
            exception.getMessage());
    }

    @Test
    void createCHDOrderThrowsApiResponseException() throws ApiErrorResponseException, URIValidationException {
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi = createMissingImageDeliveryRequestApi();

        when(mockPrivateChdOrderResourceHandler
            .postChdOrder(POST_MISSING_IMAGE_CHD_ORDER_URI, missingImageDeliveryRequestApi))
            .thenReturn(mockChdOrdersPost);
        when(mockChdOrdersPost.execute()).thenThrow(ApiErrorResponseException.class);

        assertThrows(ServiceException.class, () -> chdOrderService.createCHDOrder(
            POST_MISSING_IMAGE_CHD_ORDER_URI, missingImageDeliveryRequestApi));
    }

    private MissingImageDeliveryRequestApi createMissingImageDeliveryRequestApi() {
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi = new MissingImageDeliveryRequestApi();
        missingImageDeliveryRequestApi.setId("id");
        missingImageDeliveryRequestApi.setCompanyName("companyName");
        missingImageDeliveryRequestApi.setCompanyNumber("00000000");
        missingImageDeliveryRequestApi.setOrderedAt(DEFAULT_DATE);
        missingImageDeliveryRequestApi.setPaymentReference("paymentReference");
        missingImageDeliveryRequestApi.setFilingHistoryCategory("filingHistoryCategory");
        missingImageDeliveryRequestApi.setFilingHistoryDescription("filingHistoryDescription");
        missingImageDeliveryRequestApi.setFilingHistoryDate("2019-01-01");
        missingImageDeliveryRequestApi.setItemCost("3");

        return missingImageDeliveryRequestApi;
    }
}
