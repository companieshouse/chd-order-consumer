package uk.gov.companieshouse.chdorderconsumer.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.model.order.chd.MissingImageDeliveryRequestApi;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;
import uk.gov.companieshouse.orders.items.Item;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ItemOrderedProcessorService {

    private static final String POST_MISSING_IMAGE_CHD_ORDER_URI = "/chd-order-api/missing-image-deliveries";

    private final CHDOrderService chdOrderService;

    public ItemOrderedProcessorService(final CHDOrderService chdOrderService) {
        this.chdOrderService = chdOrderService;
    }


    public void processItemOrdered(ChdItemOrdered chdItemOrdered) {
        Item item = chdItemOrdered.getItem();
        Map<String, String> itemOptions = item.getItemOptions();

        LocalDateTime date = LocalDateTime.parse(chdItemOrdered.getOrderedAt());
        System.out.println(date);
        System.out.println(date.toString());
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi = new MissingImageDeliveryRequestApi();
        missingImageDeliveryRequestApi.setId(item.getId());
        missingImageDeliveryRequestApi.setCompanyName(item.getCompanyName());
        missingImageDeliveryRequestApi.setCompanyNumber(item.getCompanyNumber());
        missingImageDeliveryRequestApi.setOrderedAt(date);
        missingImageDeliveryRequestApi.setPaymentReference(chdItemOrdered.getPaymentReference());
        missingImageDeliveryRequestApi.setFilingHistoryCategory(itemOptions.get("filingHistoryCategory"));
        missingImageDeliveryRequestApi.setFilingHistoryDate(itemOptions.get("filingHistoryDate"));
        missingImageDeliveryRequestApi.setFilingHistoryDescription(itemOptions.get("filingHistoryDescription"));
        missingImageDeliveryRequestApi.setItemCost(item.getTotalItemCost());

        chdOrderService.createCHDOrder(POST_MISSING_IMAGE_CHD_ORDER_URI, missingImageDeliveryRequestApi);
    }
}
