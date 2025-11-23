package codingdojo.sync;

import codingdojo.common.CustomerTestDataBuilder;
import codingdojo.data.CustomerDataLayer;
import codingdojo.model.Customer;
import codingdojo.model.CustomerType;
import codingdojo.model.ExternalCustomer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class CustomerSyncTest {

    private final CustomerDataLayer db = mock(CustomerDataLayer.class);

    private CustomerSync customerSync;

    @BeforeEach
    public void setup() {
        customerSync = new CustomerSync(db);
    }

    @Test
    public void syncCompanyByExternalId(){
        // arrange
        String externalId = "12345";

        ExternalCustomer externalCustomer = CustomerTestDataBuilder.createExternalCompany();
        externalCustomer.setExternalId(externalId);

        Customer customer = CustomerTestDataBuilder.createCustomerWithSameCompanyAs(externalCustomer);
        customer.setExternalId(externalId);

        when(db.findByExternalId(externalId)).thenReturn(customer);
        when(db.updateCustomerRecord(customer)).thenReturn(customer);


        // act
        boolean created = customerSync.syncWithDataLayer(externalCustomer);

        // assert
        assertFalse(created);
        ArgumentCaptor<Customer> argument = ArgumentCaptor.forClass(Customer.class);
        verify(db, times(2)).updateCustomerRecord(argument.capture());
        Customer updatedCustomer = argument.getValue();
        assertEquals(externalCustomer.getName(), updatedCustomer.getName());
        assertEquals(externalCustomer.getExternalId(), updatedCustomer.getExternalId());
        assertNull(updatedCustomer.getMasterExternalId());
        assertEquals(externalCustomer.getCompanyNumber(), updatedCustomer.getCompanyNumber());
        assertEquals(externalCustomer.getAddress(), updatedCustomer.getAddress());
        assertEquals(externalCustomer.getShoppingLists(), updatedCustomer.getShoppingLists());
        assertEquals(CustomerType.COMPANY, updatedCustomer.getCustomerType());
        assertNull(updatedCustomer.getPreferredStore());
    }

    @Test
    public void syncWithDataLayerCallsSyncDuplicateCustomerWhenDuplicatesExist(){

        // arrange
        String externalId = "12345";
        ExternalCustomer externalCustomer = CustomerTestDataBuilder.createExternalCompany();
        Customer customer = CustomerTestDataBuilder.createCustomerWithSameCompanyAs(externalCustomer);
        Customer duplicate = CustomerTestDataBuilder.createCustomerWithSameCompanyAs(externalCustomer);
        duplicate.setExternalId(externalId);

        when(db.findByExternalId(anyString())).thenReturn(customer);
        when(db.findByMasterExternalId(anyString())).thenReturn(duplicate);
        when(db.updateCustomerRecord(customer)).thenReturn(customer);
        when(db.updateCustomerRecord(duplicate)).thenReturn(duplicate);

        // act
        customerSync.syncWithDataLayer(externalCustomer);

        // assert
        verify(db, times(1)).updateCustomerRecord(duplicate);
    }

}
