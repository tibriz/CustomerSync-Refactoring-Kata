package codingdojo.common;

import codingdojo.model.Address;
import codingdojo.model.Customer;
import codingdojo.model.CustomerType;
import codingdojo.model.ExternalCustomer;
import codingdojo.model.ShoppingList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;

/**
 * Helper class for creating test data objects in customer sync tests.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerTestDataBuilder {

    public static final String EXTERNAL_ID = "12345";
    public static final String INTERNAL_ID = "45435";
    public static final String COMPANY_NUMBER = "470813-8895";
    public static final String NAME = "Acme Inc.";

    public static ExternalCustomer createExternalCustomer(boolean isCompany) {
        ExternalCustomer externalCustomer = new ExternalCustomer();
        externalCustomer.setExternalId(EXTERNAL_ID);
        externalCustomer.setName(NAME);
        externalCustomer.setAddress(new Address("123 main st", "Helsingborg", "SE-123 45"));
        externalCustomer.setCompanyNumber(isCompany ? COMPANY_NUMBER : null);
        externalCustomer.setShoppingLists(Collections.singletonList(new ShoppingList("lipstick", "blusher")));
        return externalCustomer;
    }

    public static ExternalCustomer createExternalCompany() {
        return createExternalCustomer(true);
    }

    public static Customer createCustomerWithSameCompanyAs(ExternalCustomer externalCustomer) {
        Customer customer = new Customer();
        customer.setCompanyNumber(externalCustomer.getCompanyNumber());
        customer.setCustomerType(CustomerType.COMPANY);
        customer.setInternalId(INTERNAL_ID);
        return customer;
    }
}
