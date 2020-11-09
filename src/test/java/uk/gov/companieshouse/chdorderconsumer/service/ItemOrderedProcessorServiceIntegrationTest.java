package uk.gov.companieshouse.chdorderconsumer.service;

import com.google.api.client.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.order.chd.MissingImageDeliveryRequestApi;
import uk.gov.companieshouse.chdorderconsumer.exception.RetryableErrorException;
import uk.gov.companieshouse.chdorderconsumer.exception.ServiceException;
import uk.gov.companieshouse.chdorderconsumer.kafka.ItemOrderedKafkaProducer;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;
import uk.gov.companieshouse.orders.items.Item;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/** Integration tests the {@link ItemOrderedProcessorService} service. */
@SpringBootTest
@EmbeddedKafka
class ItemOrderedProcessorServiceIntegrationTest {

    private static final ChdItemOrdered CHD_ITEM_ORDERED;

    @Autowired
    private ItemOrderedProcessorService processorServiceUnderTest;

    @MockBean
    private CHDOrderService chdOrderService;

    @MockBean
    private ItemOrderedKafkaProducer producer;

    @MockBean
    private MongoService mockMongoService;

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
        itemOptions.put("filingHistoryBarcode", "0006594");
        item.setItemOptions(itemOptions);

        CHD_ITEM_ORDERED = new ChdItemOrdered();
        CHD_ITEM_ORDERED.setOrderedAt("2020-10-27T09:39:10.873");
        CHD_ITEM_ORDERED.setPaymentReference("payment ref");
        CHD_ITEM_ORDERED.setItem(item);
    }

    @Test
    @DisplayName("processItemOrdered() propagates non-retryable ServiceException so consumer can handle it accordingly")
    void propagatesNonRetryableServiceException() throws Exception {

        // Given we have a response that returns 401
        ApiResponse<MissingImageDeliveryRequestApi> apiResponse =
                new ApiResponse<>(UNAUTHORIZED.value(), new HttpHeaders(),
                        new MissingImageDeliveryRequestApi());
        when(chdOrderService.createCHDOrder(any(), any())).thenReturn(apiResponse);

        // When and then
        assertThatExceptionOfType(ServiceException.class).isThrownBy(() ->
                processorServiceUnderTest.processItemOrdered(CHD_ITEM_ORDERED))
                .withMessageContaining("statusCode=401")
                .withNoCause();
    }

    @Test
    @DisplayName("processItemOrdered() propagates retryable RetryableErrorException so consumer can retry")
    void propagatesRetryableErrorException() throws Exception {

        // Given we have a response that returns 500
        ApiResponse<MissingImageDeliveryRequestApi> apiResponse =
                new ApiResponse<>(INTERNAL_SERVER_ERROR.value(), new HttpHeaders(),
                        new MissingImageDeliveryRequestApi());
        when(chdOrderService.createCHDOrder(any(), any())).thenReturn(apiResponse);

        // When and then
        assertThatExceptionOfType(RetryableErrorException.class).isThrownBy(() ->
                processorServiceUnderTest.processItemOrdered(CHD_ITEM_ORDERED))
                .withMessageContaining("statusCode=500")
                .withNoCause();
    }
}
