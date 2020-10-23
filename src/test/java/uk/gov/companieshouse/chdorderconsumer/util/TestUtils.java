package uk.gov.companieshouse.chdorderconsumer.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;
import uk.gov.companieshouse.orders.items.Item;
import uk.gov.companieshouse.orders.items.ItemCosts;
import uk.gov.companieshouse.orders.items.Links;
import uk.gov.companieshouse.orders.items.OrderedBy;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.companieshouse.chdorderconsumer.util.TestConstants.MISSING_IMAGE_DELIVERY_ITEM_ID;
import static uk.gov.companieshouse.chdorderconsumer.util.TestConstants.ORDER_REFERENCE;

public class TestUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Creates a valid single MID item order with all the required fields populated.
     * @return a fully populated {@link ChdItemOrdered} object
     */
    public static ChdItemOrdered createOrder() {
        final ChdItemOrdered order = new ChdItemOrdered();
        final Item item = new Item();
        item.setId(MISSING_IMAGE_DELIVERY_ITEM_ID);
        order.setItem(item);
        order.setOrderedAt(LocalDateTime.now().toString());
        final OrderedBy orderedBy = new OrderedBy();
        orderedBy.setEmail("demo@ch.gov.uk");
        orderedBy.setId("4Y2VkZWVlMzhlZWFjY2M4MzQ3M1234");
        order.setOrderedBy(orderedBy);
        final ItemCosts costs = new ItemCosts("0", "3", "3", "missing-image-delivery-accounts");
        item.setItemCosts(singletonList(costs));
        final Map<String, String> options = new HashMap<>();
        options.put("filingHistoryDescriptionValues",
                "{\"change_date\":\"2010-02-12\",\"officer_name\":\"Thomas David Wheare\"}");
        options.put("filingHistoryCategory", "officers");
        options.put("filingHistoryDate", "2010-02-12");
        options.put("filingHistoryDescription", "change-person-director-company-with-change-date");
        options.put("filingHistoryId", "MzAwOTM2MDg5OWFkaXF6a2N4");
        options.put("filingHistoryType", "CH01");
        item.setItemOptions(options);
        final Links links = new Links();
        links.setSelf("/orderable/missing-image-deliveries/MID-535516-028321");
        item.setLinks(links);
        item.setQuantity(1);
        item.setCompanyName("THE GIRLS' DAY SCHOOL TRUST");
        item.setCompanyNumber("00006400");
        item.setCustomerReference("MID ordered by VJ GCI-1301");
        item.setDescription("missing image delivery for company 00006400");
        item.setDescriptionIdentifier("missing-image-delivery");
        final Map<String, String> descriptionValues = new HashMap<>();
        descriptionValues.put("company_number", "00006400");
        descriptionValues.put("missing-image-delivery", "missing image delivery for company 00006400");
        item.setDescriptionValues(descriptionValues);
        item.setItemUri("/orderable/missing-image-deliveries/MID-535516-028321");
        item.setKind("item#missing-image-delivery");
        item.setTotalItemCost("3");
        item.setPostageCost("0");
        order.setPaymentReference("1234");
        order.setReference(ORDER_REFERENCE);
        order.setTotalOrderCost("3");
        return order;
    }

    /**
     * Asserts that two JSON strings are equal, ignoring any differences in the ordering of fields.
     * @param json1 the first JSON string
     * @param json2 the second JSON string
     * @throws IOException should something unexpected happen.
     */
    public static void assertJsonsEqualIgnoringFieldOrdering(final String json1, final String json2) throws IOException  {
        final JsonNode payloadJsonNode = MAPPER.readTree(json1);
        final JsonNode orderJsonNode = MAPPER.readTree(json2);
        assertThat(payloadJsonNode.equals(orderJsonNode), is(true));
    }

}
