package codingdojo.data;

import codingdojo.model.Customer;
import codingdojo.model.CustomerType;
import codingdojo.model.ExternalCustomer;
import codingdojo.model.ShoppingList;
import codingdojo.model.CustomerMatches;
import codingdojo.model.CustomerSyncResult;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
public class CustomerDataAccess {

    private final CustomerDataLayer customerDataLayer;

    public CustomerMatches loadCompanyCustomer(String externalId, String companyNumber) {
        CustomerMatches matches = new CustomerMatches();
        Customer matchByExternalId = this.customerDataLayer.findByExternalId(externalId);
        if (matchByExternalId != null) {
            matches.setCustomer(matchByExternalId);
            matches.setMatchTerm("ExternalId");
            Customer matchByMasterId = this.customerDataLayer.findByMasterExternalId(externalId);
            if (matchByMasterId != null) matches.addDuplicate(matchByMasterId);
        } else {
            Customer matchByCompanyNumber = this.customerDataLayer.findByCompanyNumber(companyNumber);
            if (matchByCompanyNumber != null) {
                matches.setCustomer(matchByCompanyNumber);
                matches.setMatchTerm("CompanyNumber");
            }
        }

        return matches;
    }

    public CustomerMatches loadPersonCustomer(String externalId) {
        CustomerMatches matches = new CustomerMatches();
        Customer matchByPersonalNumber = this.customerDataLayer.findByExternalId(externalId);
        matches.setCustomer(matchByPersonalNumber);
        if (matchByPersonalNumber != null) matches.setMatchTerm("ExternalId");
        return matches;
    }

    public Customer updateCustomerRecord(Customer customer) {
        return customerDataLayer.updateCustomerRecord(customer);
    }

    public Customer createCustomerRecord(Customer customer) {
        return customerDataLayer.createCustomerRecord(customer);
    }

    /**
     * Synchronizes a customer with external customer data.
     * Handles creation of new customers and updates to existing ones.
     * 
     * @param customer existing customer or null for new customers
     * @param externalCustomer external customer data to sync
     * @return the synchronization result
     */
    public CustomerSyncResult syncCustomer(Customer customer, ExternalCustomer externalCustomer) {

        if (customer == null) {
            customer = new Customer();
            customer.setExternalId(externalCustomer.getExternalId());
            customer.setMasterExternalId(externalCustomer.getExternalId());
        }

        populateCustomerFields(customer, externalCustomer);

        boolean created = false;
        if (customer.getInternalId() == null) {
            customer = createCustomerRecord(customer);
            created = true;
        } else {
            customer = updateCustomerRecord(customer);
        }

        return new CustomerSyncResult(customer, created);
    }

    /**
     * Synchronizes a duplicate customer with external customer data.
     * 
     * @param duplicate the duplicate customer to synchronize
     * @param externalCustomer external customer data to sync
     */
    public void syncDuplicateCustomer(Customer duplicate, ExternalCustomer externalCustomer) {
        if (duplicate == null) {
            duplicate = new Customer();
            duplicate.setExternalId(externalCustomer.getExternalId());
            duplicate.setMasterExternalId(externalCustomer.getExternalId());
        }

        duplicate.setName(externalCustomer.getName());

        // Update bonus points for persons only
        if (!externalCustomer.isCompany()) {
            updateBonusPointsBalance(externalCustomer, duplicate);
        }

        if (duplicate.getInternalId() == null) {
            createCustomerRecord(duplicate);
        } else {
            updateCustomerRecord(duplicate);
        }
    }

    /**
     * Synchronizes shopping lists for a customer.
     * 
     * @param customer the customer to update
     * @param shoppingLists the shopping lists from external customer
     */
    public void syncShoppingLists(Customer customer, List<ShoppingList> shoppingLists) {
        if (shoppingLists == null || shoppingLists.isEmpty()) {
            return;
        }

        for (ShoppingList shoppingList : shoppingLists) {
            customer.addShoppingList(shoppingList);
            customerDataLayer.updateShoppingList(shoppingList);
        }
        customerDataLayer.updateCustomerRecord(customer);
    }

    /**
     * Populates customer fields from an external customer.
     * 
     * @param customer the customer to populate
     * @param externalCustomer the source external customer
     */
    private void populateCustomerFields(Customer customer, ExternalCustomer externalCustomer) {
        customer.setName(externalCustomer.getName());
        customer.setAddress(externalCustomer.getAddress());
        customer.setPreferredStore(externalCustomer.getPreferredStore());

        if (externalCustomer.isCompany()) {
            customer.setCompanyNumber(externalCustomer.getCompanyNumber());
            customer.setCustomerType(CustomerType.COMPANY);
        } else {
            customer.setCustomerType(CustomerType.PERSON);
            updateBonusPointsBalance(externalCustomer, customer);
        }
    }

    private void updateBonusPointsBalance(ExternalCustomer externalCustomer, Customer customer) {
        // Only update bonus points if they differ from the stored value
        if (customer.getBonusPointsBalance() != externalCustomer.getBonusPointsBalance()) {
            customer.setBonusPointsBalance(externalCustomer.getBonusPointsBalance());
        }
    }
}
