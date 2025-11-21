package codingdojo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;

import static codingdojo.CustomerTestDataBuilder.EXTERNAL_ID;
import static codingdojo.CustomerTestDataBuilder.INTERNAL_ID;
import static codingdojo.CustomerTestDataBuilder.NAME;
import static codingdojo.CustomerTestDataBuilder.createCustomerWithSameCompanyAs;
import static codingdojo.CustomerTestDataBuilder.createExternalCustomer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class CustomerSyncUpdateDuplicateTest {

    private CustomerSync customerSync;
    private CustomerDataLayer db;
    private ArgumentCaptor<Customer> customerCaptor;

    private Method updateDuplicateMethod;

    @BeforeEach
    public void setup() throws NoSuchMethodException {
        db = mock(CustomerDataLayer.class);
        customerSync = new CustomerSync(db);
        customerCaptor = ArgumentCaptor.forClass(Customer.class);

        updateDuplicateMethod = CustomerSync.class.getDeclaredMethod("updateDuplicate", ExternalCustomer.class, Customer.class);
        updateDuplicateMethod.setAccessible(true);
    }

    @Test
    public void updateDuplicateCreatesNewCustomerWhenNull() throws Exception {

        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);

        // act
        updateDuplicateMethod.invoke(customerSync, externalCustomer, null);

        // assert
        verify(db, times(1)).createCustomerRecord(customerCaptor.capture());
        
        Customer createdDuplicate = customerCaptor.getValue();
        assertEquals(EXTERNAL_ID, createdDuplicate.getExternalId(), "Created duplicate should have external ID");
        assertEquals(NAME, createdDuplicate.getName(), "Created duplicate should have the name from external customer");
    }

    @Test
    public void updateDuplicateUpdatesName() throws Exception {

        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);
        Customer duplicateCustomer = createCustomerWithSameCompanyAs(externalCustomer);

        // act
        updateDuplicateMethod.invoke(customerSync, externalCustomer, duplicateCustomer);

        // assert
        verify(db).updateCustomerRecord(customerCaptor.capture());
        
        Customer updated = customerCaptor.getValue();
        assertEquals(INTERNAL_ID, updated.getInternalId(), "Updated duplicate should retain internal ID");
        assertEquals(NAME, updated.getName(), "Updated duplicate should have new name");
    }

    @Test
    public void updateDuplicateCompanyDoesNotUpdateBonusPoints() throws Exception {
        //arrange
        int externalBonusPoints = 8000;
        int originalBonusPoints = 2000;

        ExternalCustomer externalCustomer = createExternalCustomer(true);
        externalCustomer.setBonusPointsBalance(externalBonusPoints);

        Customer customer = createCustomerWithSameCompanyAs(externalCustomer);
        customer.setInternalId("dupCompanyId");
        customer.setBonusPointsBalance(originalBonusPoints);

        // act
        updateDuplicateMethod.invoke(customerSync, externalCustomer, customer);

        // assert
        verify(db).updateCustomerRecord(customerCaptor.capture());
        
        Customer updated = customerCaptor.getValue();
        assertEquals(originalBonusPoints, updated.getBonusPointsBalance(), 
            "Company should NOT have bonus points updated");
    }

    @Test
    public void updateDuplicatePrivatePeopleUpdateBonusPoints() throws Exception {
        //arrange
        int externalBonusPoints = 1000;
        int originalBonusPoints = 2000;

        ExternalCustomer externalCustomer = createExternalCustomer(false);
        externalCustomer.setBonusPointsBalance(externalBonusPoints);

        Customer customer = createCustomerWithSameCompanyAs(externalCustomer);
        customer.setBonusPointsBalance(originalBonusPoints);

        // act
        updateDuplicateMethod.invoke(customerSync, externalCustomer, customer);

        // assert
        verify(db).updateCustomerRecord(customerCaptor.capture());

        Customer updated = customerCaptor.getValue();
        assertEquals(externalBonusPoints, updated.getBonusPointsBalance(),
                "Company should NOT have bonus points updated");
    }

    @Test
    public void updateDuplicateCreatesWhenNoInternalId() throws Exception {

        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);
        Customer customer = createCustomerWithSameCompanyAs(externalCustomer);
        customer.setInternalId(null);

        // act
        updateDuplicateMethod.invoke(customerSync, externalCustomer, customer);

        // assert
        verify(db).createCustomerRecord(customerCaptor.capture());
        
        Customer created = customerCaptor.getValue();
        assertEquals(NAME, created.getName(), "Created duplicate should have new name");
    }

    @Test
    public void updateDuplicateUpdatesWhenHasInternalId() throws Exception {

        // arrange
        ExternalCustomer externalCustomer = createExternalCustomer(true);
        Customer duplicateWithId = createCustomerWithSameCompanyAs(externalCustomer);

        // act
        updateDuplicateMethod.invoke(customerSync, externalCustomer, duplicateWithId);

        // assert
        verify(db).updateCustomerRecord(customerCaptor.capture());
        
        Customer updated = customerCaptor.getValue();
        assertEquals(INTERNAL_ID, updated.getInternalId(), "Updated duplicate should retain internal ID");
        assertEquals(NAME, updated.getName(), "Updated duplicate should have new name");
    }

}

