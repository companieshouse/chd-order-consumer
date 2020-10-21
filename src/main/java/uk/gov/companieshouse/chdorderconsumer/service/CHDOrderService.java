package uk.gov.companieshouse.chdorderconsumer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriTemplate;
import uk.gov.companieshouse.api.ApiClient;

@Service
public class CHDOrderService {

    private final ApiClientService apiClientService;

    private static final String POST_MISSING_IMAGE_CHD_ORDER_URI = "/chd-order-api/missing-image-deliveries";

    public CHDOrderService(final ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    public void createCHDOrder() {

        final ApiClient apiClient = apiClientService.getInternalApiClient();
        final String uri = POST_MISSING_IMAGE_CHD_ORDER_URI;

        // Extract the contents of the message and populate the DTO

        // POST DTO

        // Handle Response

        // Make sure to add plenty of logging
    }
}
