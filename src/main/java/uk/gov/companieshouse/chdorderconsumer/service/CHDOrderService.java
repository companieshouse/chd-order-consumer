package uk.gov.companieshouse.chdorderconsumer.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.order.chd.MissingImageDeliveryRequestApi;
import uk.gov.companieshouse.chdorderconsumer.exception.RetryableErrorException;
import uk.gov.companieshouse.chdorderconsumer.exception.ServiceException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class CHDOrderService {

    private final ApiClientService apiClientService;

    public CHDOrderService(final ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    public ApiResponse<MissingImageDeliveryRequestApi> createCHDOrder(String uri,
        MissingImageDeliveryRequestApi missingImageDeliveryRequestApi) throws ApiErrorResponseException {

        final InternalApiClient apiClient = apiClientService.getInternalApiClient();

        try {
            return apiClient.privateChdOrderResourceHandler()
                    .postChdOrder(uri, missingImageDeliveryRequestApi)
                    .execute();
        } catch (URIValidationException ex) {
            throw new ServiceException("Unrecognised uri pattern for: " + uri);
        }
    }
}
