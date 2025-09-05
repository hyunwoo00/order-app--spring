package company.orderApp.controller.util;

import company.orderApp.domain.order.Receipt;

import java.util.Objects;

public class ReceiptFactory {
    public static Receipt issueReceipt(String receiptType) {
        if(Objects.equals(receiptType, "cash_receipt")){
            return Receipt.CASH_RECEIPT;
        }
        else{
            return Receipt.TAX_INVOICE;
        }
    }
}
