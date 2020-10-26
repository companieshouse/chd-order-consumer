package uk.gov.companieshouse.chdorderconsumer.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.order.chd.MissingImageDeliveryRequestApi;
import uk.gov.companieshouse.chdorderconsumer.exception.ServiceException;

@Service
public class CHDOrderService {

    private final ApiClientService apiClientService;

    public CHDOrderService(final ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    public ApiResponse<MissingImageDeliveryRequestApi> createCHDOrder(String uri,
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi) {

        final InternalApiClient apiClient = apiClientService.getInternalApiClient();

        try {
            return apiClient.privateChdOrderResourceHandler()
                    .postChdOrder(uri, missingImageDeliveryRequestApi)
                    .execute();
        } catch (URIValidationException ex) {
            throw new ServiceException("Unrecognised uri pattern for: " + uri);
        } catch (ApiErrorResponseException ex) {
            throw new ServiceException("API Response Error for : "
                + missingImageDeliveryRequestApi.getId() + ", Error response: " + ex.toString());
        }
    }
}
