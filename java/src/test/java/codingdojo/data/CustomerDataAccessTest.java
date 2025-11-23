package codingdojo.data;

import codingdojo.model.Customer;
import codingdojo.model.CustomerSyncResult;
import codingdojo.model.ExternalCustomer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static codingdojo.common.CustomerTestDataBuilder.EXTERNAL_ID;
import static codingdojo.common.CustomerTestDataBuilder.INTERNAL_ID;
import static codingdojo.common.CustomerTestDataBuilder.NAME;
import static codingdojo.common.CustomerTestDataBuilder.createCustomerWithSameCompanyAs;
import static codingdojo.common.CustomerTestDataBuilder.createExternalCustomer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class CustomerDataAccessTest {

    private CustomerDataAccess customerDataAccess;
    private CustomerDataLayer db;
    private ArgumentCaptor<Customer> customerCaptor;

    @BeforeEach
    public void setup() {
        db = mock(CustomerDataLayer.class);
        customerDataAccess = new CustomerDataAccess(db);
        customerCaptor = ArgumentCaptor.forClass(Customer.class);
    }

    @Test
    public void syncCustomerCreatesNewCustomerWhenNull() {
        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);
        Customer createdCustomer = new Customer();
        createdCustomer.setInternalId("newId");
        when(db.createCustomerRecord(customerCaptor.capture())).thenReturn(createdCustomer);

        // act
        CustomerSyncResult result = customerDataAccess.syncCustomer(null, externalCustomer);

        // assert
        assertTrue(result.isCreated(), "Result should indicate customer was created");
        assertEquals(createdCustomer, result.getCustomer(), "Result should contain created customer");
        verify(db, times(1)).createCustomerRecord(customerCaptor.capture());
        
        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals(EXTERNAL_ID, capturedCustomer.getExternalId(), "New customer should have external ID");
        assertEquals(EXTERNAL_ID, capturedCustomer.getMasterExternalId(), "New customer should have master external ID");
        assertEquals(NAME, capturedCustomer.getName(), "New customer should have external customer's name");
    }

    @Test
    public void syncCustomerUpdatesExistingCustomerWithInternalId() {
        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);
        Customer existingCustomer = new Customer();
        existingCustomer.setInternalId(INTERNAL_ID);
        existingCustomer.setExternalId(EXTERNAL_ID);
        
        Customer updatedCustomer = new Customer();
        updatedCustomer.setInternalId(INTERNAL_ID);
        when(db.updateCustomerRecord(customerCaptor.capture())).thenReturn(updatedCustomer);

        // act
        CustomerSyncResult result = customerDataAccess.syncCustomer(existingCustomer, externalCustomer);

        // assert
        assertFalse(result.isCreated(), "Result should indicate customer was not created");
        assertEquals(updatedCustomer, result.getCustomer(), "Result should contain updated customer");
        verify(db, times(1)).updateCustomerRecord(customerCaptor.capture());
        
        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals(NAME, capturedCustomer.getName(), "Updated customer should have new name from external customer");
    }

    @Test
    public void syncCustomerPopulatesCompanyFields() {
        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);
        Customer existingCustomer = new Customer();
        existingCustomer.setInternalId(INTERNAL_ID);
        
        Customer updatedCustomer = new Customer();
        when(db.updateCustomerRecord(customerCaptor.capture())).thenReturn(updatedCustomer);

        // act
        customerDataAccess.syncCustomer(existingCustomer, externalCustomer);

        // assert
        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals(externalCustomer.getAddress(), capturedCustomer.getAddress(), "Address should be populated");
        assertEquals(externalCustomer.getPreferredStore(), capturedCustomer.getPreferredStore(), "Preferred store should be populated");
        assertEquals(externalCustomer.getCompanyNumber(), capturedCustomer.getCompanyNumber(), "Company number should be populated for company");
    }

    @Test
    public void syncCustomerHandlesPersonCustomer() {
        // arrange
        ExternalCustomer externalPerson = createExternalCustomer(false);
        externalPerson.setBonusPointsBalance(1500);
        
        Customer existingCustomer = new Customer();
        existingCustomer.setInternalId(INTERNAL_ID);
        
        Customer updatedCustomer = new Customer();
        when(db.updateCustomerRecord(customerCaptor.capture())).thenReturn(updatedCustomer);

        // act
        customerDataAccess.syncCustomer(existingCustomer, externalPerson);

        // assert
        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals(1500, capturedCustomer.getBonusPointsBalance(), "Person customer should have bonus points updated");
    }

    @Test
    public void syncCustomerCreatesNewPersonCustomerWithoutInternalId() {
        // arrange
        ExternalCustomer externalPerson = createExternalCustomer(false);
        externalPerson.setBonusPointsBalance(2000);
        
        Customer createdCustomer = new Customer();
        createdCustomer.setInternalId("newPersonId");
        when(db.createCustomerRecord(customerCaptor.capture())).thenReturn(createdCustomer);

        // act
        CustomerSyncResult result = customerDataAccess.syncCustomer(null, externalPerson);

        // assert
        assertTrue(result.isCreated(), "Result should indicate customer was created");
        verify(db, times(1)).createCustomerRecord(customerCaptor.capture());
        
        Customer capturedCustomer = customerCaptor.getValue();
        assertEquals(EXTERNAL_ID, capturedCustomer.getExternalId(), "Created person should have external ID");
        assertEquals(2000, capturedCustomer.getBonusPointsBalance(), "Created person should have bonus points");
    }

    @Test
    public void updateDuplicateCreatesNewCustomerWhenNull() {

        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);

        // act
        customerDataAccess.syncDuplicateCustomer(null, externalCustomer);

        // assert
        verify(db, times(1)).createCustomerRecord(customerCaptor.capture());
        
        Customer createdDuplicate = customerCaptor.getValue();
        assertEquals(EXTERNAL_ID, createdDuplicate.getExternalId(), "Created duplicate should have external ID");
        assertEquals(NAME, createdDuplicate.getName(), "Created duplicate should have the name from external customer");
    }

    @Test
    public void updateDuplicateUpdatesName() {

        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);
        Customer duplicateCustomer = createCustomerWithSameCompanyAs(externalCustomer);

        // act
        customerDataAccess.syncDuplicateCustomer(duplicateCustomer, externalCustomer);

        // assert
        verify(db).updateCustomerRecord(customerCaptor.capture());
        
        Customer updated = customerCaptor.getValue();
        assertEquals(INTERNAL_ID, updated.getInternalId(), "Updated duplicate should retain internal ID");
        assertEquals(NAME, updated.getName(), "Updated duplicate should have new name");
    }

    @Test
    public void updateDuplicateCompanyDoesNotUpdateBonusPoints() {
        //arrange
        int externalBonusPoints = 8000;
        int originalBonusPoints = 2000;

        ExternalCustomer externalCustomer = createExternalCustomer(true);
        externalCustomer.setBonusPointsBalance(externalBonusPoints);

        Customer customer = createCustomerWithSameCompanyAs(externalCustomer);
        customer.setInternalId("dupCompanyId");
        customer.setBonusPointsBalance(originalBonusPoints);

        // act
        customerDataAccess.syncDuplicateCustomer(customer, externalCustomer);

        // assert
        verify(db).updateCustomerRecord(customerCaptor.capture());
        
        Customer updated = customerCaptor.getValue();
        assertEquals(originalBonusPoints, updated.getBonusPointsBalance(), 
            "Company should NOT have bonus points updated");
    }

    @Test
    public void updateDuplicatePrivatePeopleUpdateBonusPoints() {
        //arrange
        int externalBonusPoints = 1000;
        int originalBonusPoints = 2000;

        ExternalCustomer externalCustomer = createExternalCustomer(false);
        externalCustomer.setBonusPointsBalance(externalBonusPoints);

        Customer customer = createCustomerWithSameCompanyAs(externalCustomer);
        customer.setBonusPointsBalance(originalBonusPoints);

        // act
        customerDataAccess.syncDuplicateCustomer(customer, externalCustomer);

        // assert
        verify(db).updateCustomerRecord(customerCaptor.capture());

        Customer updated = customerCaptor.getValue();
        assertEquals(externalBonusPoints, updated.getBonusPointsBalance(),
                "Company should NOT have bonus points updated");
    }

    @Test
    public void updateDuplicateCreatesWhenNoInternalId() {

        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);
        Customer customer = createCustomerWithSameCompanyAs(externalCustomer);
        customer.setInternalId(null);

        // act
        customerDataAccess.syncDuplicateCustomer(customer, externalCustomer);

        // assert
        verify(db).createCustomerRecord(customerCaptor.capture());
        
        Customer created = customerCaptor.getValue();
        assertEquals(NAME, created.getName(), "Created duplicate should have new name");
    }

    @Test
    public void updateDuplicateUpdatesWhenHasInternalId() {

        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);
        Customer duplicateWithId = createCustomerWithSameCompanyAs(externalCustomer);

        // act
        customerDataAccess.syncDuplicateCustomer(duplicateWithId, externalCustomer);

        // assert
        verify(db).updateCustomerRecord(customerCaptor.capture());
        
        Customer updated = customerCaptor.getValue();
        assertEquals(INTERNAL_ID, updated.getInternalId(), "Updated duplicate should retain internal ID");
        assertEquals(NAME, updated.getName(), "Updated duplicate should have new name");
    }

}
