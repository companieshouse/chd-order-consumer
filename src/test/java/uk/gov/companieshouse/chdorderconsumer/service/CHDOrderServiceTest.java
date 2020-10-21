package uk.gov.companieshouse.chdorderconsumer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CHDOrderServiceTest {

    @InjectMocks
    private CHDOrderService chdOrderService;

    @Mock
    private ApiClientService mockApiClientService;

    @Mock
    private InternalApiClient mockApiClient;

    @Test
    void createCHDOrderSuccessful() {
        when(mockApiClientService.getInternalApiClient()).thenReturn(mockApiClient);

    }
}
