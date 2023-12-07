package co.poynt.samples.codesamples.utils;

import com.godaddy.commerce.models.order.Address;
import com.godaddy.commerce.models.order.Billing;
import com.godaddy.commerce.models.order.FulfillmentMode;
import com.godaddy.commerce.models.order.FulfillmentStatus;
import com.godaddy.commerce.models.order.HistoryEvent;
import com.godaddy.commerce.models.order.LineItem;
import com.godaddy.commerce.models.order.LineItemDetails;
import com.godaddy.commerce.models.order.LineItemTotals;
import com.godaddy.commerce.models.order.LineItemType;
import com.godaddy.commerce.models.order.Order;
import com.godaddy.commerce.models.order.OrderContext;
import com.godaddy.commerce.models.order.OrderStatus;
import com.godaddy.commerce.models.order.OrderStatuses;
import com.godaddy.commerce.models.order.OrderTotals;
import com.godaddy.commerce.models.order.PaymentStatus;
import com.godaddy.commerce.models.order.Shipping;
import com.godaddy.commerce.models.order.ShippingLine;
import com.godaddy.commerce.models.order.ShippingLineTotals;
import com.godaddy.commerce.models.order.common.LinkMinusDescription;
import com.godaddy.commerce.models.order.common.SimpleMinusMoney;
import com.godaddy.commerce.models.order.common.Tax;

import java.util.ArrayList;
import java.util.List;

public class CentralOrderUtils {
    public static Order getCentralOrder() {
        Order order = new Order();

        order.setId("Order_2RALixHX2FqTAAmtlZ3YoMmqzRo");
        order.setExternalId("2916779365");
        order.setCreatedAt("2023-06-13T20:27:49.092Z");
        order.setUpdatedAt("2023-06-13T20:27:49.092Z");
        order.setNumber("1004");
        order.setNumberDisplay("2916779365");
        order.setProcessedAt("2023-06-13T18:28:01.000Z");

        // Set history
        List<HistoryEvent> historyList = new ArrayList<>();
        HistoryEvent history = new HistoryEvent();
        history.setEventName("ORDER.CREATED");
        history.setEventData("{\"actionOwner\":\"urn:com.godaddy:commerce.order\"}");
        history.setCreatedAt("2023-06-13T20:27:49.059Z");
        historyList.add(history);
        order.setHistory(historyList);

        // Set line items
        List<LineItem> lineItems = new ArrayList<>();

        LineItem lineItem = new LineItem();
        lineItem.setId("LineItem_2RALiutgyhS1JljSG8lVsXCz5Un");
        lineItem.setType(LineItemType.PHYSICAL);
        lineItem.setName("Plush 1");
        lineItem.setFulfillmentMode(FulfillmentMode.SHIP);
        lineItem.setStatus(FulfillmentStatus.UNFULFILLED);

        LineItemTotals totals = new LineItemTotals();

        SimpleMinusMoney discountTotal = new SimpleMinusMoney();
        discountTotal.setCurrencyCode("USD");
        discountTotal.setValue(0l);
        totals.setDiscountTotal(discountTotal);

        SimpleMinusMoney feeTotal = new SimpleMinusMoney();
        feeTotal.setCurrencyCode("USD");
        feeTotal.setValue(0l);
        totals.setFeeTotal(feeTotal);


        SimpleMinusMoney taxTotal = new SimpleMinusMoney();
        taxTotal.setCurrencyCode("USD");
        taxTotal.setValue(0l);
        totals.setTaxTotal(taxTotal);

        SimpleMinusMoney subTotal = new SimpleMinusMoney();
        subTotal.setCurrencyCode("USD");
        subTotal.setValue(1599l);
        totals.setSubTotal(subTotal);

        lineItem.setTotals(totals);

        SimpleMinusMoney unitAmount = new SimpleMinusMoney();
        unitAmount.setCurrencyCode("USD");
        unitAmount.setValue(1599l);
        lineItem.setUnitAmount(unitAmount);

        lineItem.setQuantity(1f);
        lineItem.setExternalId("3598891661");

        LineItemDetails details = new LineItemDetails();
        details.setSku("Pokeball_Ult");
        lineItem.setDetails(details);

        List<Tax> taxes = new ArrayList<>();
        Tax tax = new Tax();
        tax.setId("2RALiw8aJNUOUmVC5HfMX55Z8oo");
        tax.setName("TAX1");
        tax.setAmount(new SimpleMinusMoney("USD", 0l));
        tax.setExempted(false);
        taxes.add(tax);

        lineItem.setTaxes(taxes);
        lineItem.setFulfilledAt(null);
        lineItem.setCreatedAt("2023-06-13T20:27:49.173Z");
        lineItem.setUpdatedAt("2023-06-13T20:27:49.173Z");


//LineItem 2

        LineItem lineItem2 = new LineItem();
        lineItem2.setId("LineItem_2RALiutgyhS1JljSG8lVsXCz5Un");
        lineItem2.setType(LineItemType.PHYSICAL);
        lineItem2.setName("Plush 2");
        lineItem2.setFulfillmentMode(FulfillmentMode.SHIP);
        lineItem2.setStatus(FulfillmentStatus.UNFULFILLED);

        LineItemTotals totals2 = new LineItemTotals();

        SimpleMinusMoney discountTotal2 = new SimpleMinusMoney();
        discountTotal2.setCurrencyCode("USD");
        discountTotal2.setValue(0l);
        totals2.setDiscountTotal(discountTotal2);

        SimpleMinusMoney feeTotal2 = new SimpleMinusMoney();
        feeTotal2.setCurrencyCode("USD");
        feeTotal2.setValue(0l);
        totals2.setFeeTotal(feeTotal2);


        SimpleMinusMoney taxTotal2 = new SimpleMinusMoney();
        taxTotal2.setCurrencyCode("USD");
        taxTotal2.setValue(0l);
        totals2.setTaxTotal(taxTotal2);

        SimpleMinusMoney subTotal2 = new SimpleMinusMoney();
        subTotal2.setCurrencyCode("USD");
        subTotal2.setValue(2000l);
        totals2.setSubTotal(subTotal2);

        lineItem2.setTotals(totals2);

        SimpleMinusMoney unitAmount2 = new SimpleMinusMoney();
        unitAmount2.setCurrencyCode("USD");
        unitAmount2.setValue(2000l);
        lineItem2.setUnitAmount(unitAmount2);

        lineItem2.setQuantity(1f);
        lineItem2.setExternalId("3598891661");

        LineItemDetails details2 = new LineItemDetails();
        details2.setSku("Pokeball_Ult");
        lineItem2.setDetails(details2);

        List<Tax> taxes2 = new ArrayList<>();
        Tax tax2 = new Tax();
        tax2.setId("2RALiw8aJNUOUmVC5HfMX55Z8oo");
        tax2.setName("TAX1");
        tax2.setAmount(new SimpleMinusMoney("USD", 0l));
        tax2.setExempted(false);
        taxes2.add(tax2);

        lineItem2.setTaxes(taxes2);
        lineItem2.setFulfilledAt(null);
        lineItem2.setCreatedAt("2023-06-13T20:27:49.173Z");
        lineItem2.setUpdatedAt("2023-06-13T20:27:49.173Z");


        lineItems.add(lineItem);
        lineItems.add(lineItem2);
        order.setLineItems(lineItems);

        // Set context
        OrderContext context = new OrderContext();
        context.setChannelId("699a1c7f-1f7e-487a-baae-141e9417c114");
        context.setVentureId("c49a825c-0a23-48f6-90a5-27f489dae315");
        context.setBusinessId("d5db7202-657a-47a5-8cb7-5176d490fc36");
        context.setStoreId("1dd52631-0095-4702-8b63-844dad969ef0");
        context.setOwner("urn:com.marketplaces:commerce.order");
        order.setContext(context);

        // Set statuses
        OrderStatuses statuses = new OrderStatuses();
        statuses.setStatus(OrderStatus.OPEN);
        statuses.setPaymentStatus(PaymentStatus.NONE);
        statuses.setFulfillmentStatus(FulfillmentStatus.UNFULFILLED);
        order.setStatuses(statuses);

        // Set totals
        OrderTotals orderTotals = new OrderTotals();
        SimpleMinusMoney orderSubTotal = new SimpleMinusMoney();
        orderSubTotal.setCurrencyCode("USD");
        orderSubTotal.setValue(3599l);
        orderTotals.setSubTotal(orderSubTotal);

        SimpleMinusMoney shippingTotal = new SimpleMinusMoney();
        shippingTotal.setCurrencyCode("USD");
        shippingTotal.setValue(935l);
        orderTotals.setShippingTotal(shippingTotal);

        SimpleMinusMoney orderTotal = new SimpleMinusMoney();
        orderTotal.setCurrencyCode("USD");
        orderTotal.setValue(4903l);
        orderTotals.setTotal(orderTotal);

        SimpleMinusMoney orderDiscountTotal = new SimpleMinusMoney();
        orderDiscountTotal.setCurrencyCode("USD");
        orderDiscountTotal.setValue(0l);
        orderTotals.setDiscountTotal(orderDiscountTotal);

        SimpleMinusMoney orderFeeTotal = new SimpleMinusMoney();
        orderFeeTotal.setCurrencyCode("USD");
        orderFeeTotal.setValue(0l);
        orderTotals.setFeeTotal(orderFeeTotal);

        SimpleMinusMoney orderTaxTotal = new SimpleMinusMoney();
        orderTaxTotal.setCurrencyCode("USD");
        orderTaxTotal.setValue(369l);
        orderTotals.setTaxTotal(orderTaxTotal);

        order.setTotals(orderTotals);

        // Set billing
        Billing billing = new Billing();
        Address billingAddress = new Address();
        billingAddress.setAddressLine1("1022 Enterprise Way");
        billingAddress.setAdminArea2("SUNNYVALE");
        billingAddress.setAdminArea1("CA");
        billingAddress.setPostalCode("94568");
        billingAddress.setCountryCode("US");
        billing.setAddress(billingAddress);
        billing.setFirstName("Sellbrite Buyer");
        billing.setEmail("towens@sellbrite.com");
        order.setBilling(billing);

        // Set shipping
        Shipping shipping = new Shipping();
        Address shippingAddress = new Address();
        shippingAddress.setAddressLine1("1022 Enterprise Way");
        shippingAddress.setAdminArea2("SUNNYVALE");
        shippingAddress.setAdminArea1("CA");
        shippingAddress.setPostalCode("94568");
        shippingAddress.setCountryCode("US");
        shipping.setAddress(shippingAddress);
        shipping.setFirstName("Sellbrite Buyer");
        order.setShipping(shipping);

        // Set shipping lines
        List<ShippingLine> shippingLines = new ArrayList<>();
        ShippingLine shippingLine = new ShippingLine();
        shippingLine.setId("2RALiwO8vb7n6cFcsmTJdegzAkt");
        shippingLine.setName("USPS Priority Mail");

        SimpleMinusMoney shippingAmount = new SimpleMinusMoney();
        shippingAmount.setCurrencyCode("USD");
        shippingAmount.setValue(935l);
        shippingLine.setAmount(shippingAmount);

        shippingLine.setRequestedProvider(null);

        ShippingLineTotals shippingLineTotals = new ShippingLineTotals();
        SimpleMinusMoney shippingLineTaxTotal = new SimpleMinusMoney();
        shippingLineTaxTotal.setCurrencyCode("USD");
        shippingLineTaxTotal.setValue(0l);
        shippingLineTotals.setTaxTotal(shippingLineTaxTotal);

        SimpleMinusMoney shippingLineSubTotal = new SimpleMinusMoney();
        shippingLineSubTotal.setCurrencyCode("USD");
        shippingLineSubTotal.setValue(935l);
        shippingLineTotals.setSubTotal(shippingLineSubTotal);

        shippingLine.setTotals(shippingLineTotals);
        shippingLine.setRequestedService("USPS Priority Mail");

        List<Tax> shippingLineTaxes = new ArrayList<>();
        Tax shippingLineTax = new Tax();
        shippingLineTax.setId("2RALizzBjFMCdO7SjnSZmXtCruV");
        shippingLineTax.setName("TAX2");
        shippingLineTax.setAmount(new SimpleMinusMoney("USD", 0l));
        shippingLineTax.setExempted(false);
        shippingLineTaxes.add(shippingLineTax);

        shippingLine.setTaxes(shippingLineTaxes);

        shippingLines.add(shippingLine);
        order.setShippingLines(shippingLines);

        // Set taxes
        List<Tax> orderTaxes = new ArrayList<>();
        Tax orderTax = new Tax();
        orderTax.setId("2RALiwp4PxRV8Agwf6SCkVVA9b4");
        orderTax.setName("TAX A");
        orderTax.setAmount(new SimpleMinusMoney("USD", 369l));
        orderTax.setExempted(false);
        orderTax.setAdditional(true);
        orderTaxes.add(orderTax);

        order.setTaxes(orderTaxes);

        order.setLinks(new ArrayList<LinkMinusDescription>());
        order.setTaxExempted(false);
        return order;
    }
}
