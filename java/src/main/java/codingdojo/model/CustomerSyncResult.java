package codingdojo.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Getter
public class CustomerSyncResult {
    private final Customer customer;
    private final boolean created;
}
