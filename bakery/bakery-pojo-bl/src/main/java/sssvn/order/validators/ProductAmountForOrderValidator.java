package sssvn.order.validators;

import java.lang.annotation.Annotation;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.from;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.orderBy;
import static ua.com.fielden.platform.entity.query.fluent.EntityQueryUtils.select;
import static ua.com.fielden.platform.utils.EntityUtils.fetch;

import java.util.List;
import java.util.stream.Stream;

import ua.com.fielden.platform.dao.QueryExecutionModel;
import ua.com.fielden.platform.entity.query.fluent.fetch;
import ua.com.fielden.platform.entity.query.model.EntityResultQueryModel;
import ua.com.fielden.platform.entity.query.model.OrderingModel;
import ua.com.fielden.platform.keygen.IKeyNumber;
import ua.com.fielden.platform.keygen.KeyNumber;
import ua.com.fielden.platform.test.ioc.UniversalConstantsForTesting;
import ua.com.fielden.platform.types.Money;
import ua.com.fielden.platform.utils.IUniversalConstants;

import sssvn.order.OrderItem;
import sssvn.product.Product;
import ua.com.fielden.platform.entity.meta.MetaProperty;
import ua.com.fielden.platform.entity.meta.impl.AbstractBeforeChangeEventHandler;
import ua.com.fielden.platform.error.Result;

public class ProductAmountForOrderValidator extends AbstractBeforeChangeEventHandler<Product> {
	public static final Integer THRASHOLD = 5;
	public static final Integer THRASHOLD_MINIMUM = 3;
    public static final String ERR_PRODUCT_AMOUNT_EXCEEDS_THRASHOLD = "Number of products added exceeds the maximum amount of 5 items in a single order.";
    public static final String WARN_PRODUCT_AMOUNT_TOO_SMALL = "Warning: product amount in a single order is too small.";


	@Override
	public Result handle(MetaProperty<Product> property, Product product, Set<Annotation> mutatorAnnotations) {
		OrderItem orderItem = property.getEntity();
		
		final EntityResultQueryModel<OrderItem> query = select(OrderItem.class).where().prop("order").eq().val(orderItem.getOrder()).model();
		final fetch<OrderItem> fetch = fetch(OrderItem.class).with("order", "product", "quantity").fetchModel();
		final OrderingModel orderBy = orderBy().prop("order.locationFrom").asc().prop("order.locationTo").asc().model();
		final QueryExecutionModel<OrderItem, EntityResultQueryModel<OrderItem>> qem = from(query).with(fetch).with(orderBy).model();
		
		try (final Stream<OrderItem> items = co(OrderItem.class).stream(qem)) {
			items.forEach(item -> System.out.printf("Order: %s, Product: %s, Quantity: %s%n", 
					item.getOrder(), item.getProduct(), item.getQuantity()));		
			}
	
		final List<OrderItem> items = co(OrderItem.class).getAllEntities(qem);
		if (items.size() > THRASHOLD) {
			return Result.failure(ERR_PRODUCT_AMOUNT_EXCEEDS_THRASHOLD);
		}
		else if (items.size() < THRASHOLD_MINIMUM) {
			return Result.warning(WARN_PRODUCT_AMOUNT_TOO_SMALL);
		}
		
		return Result.successful(product);
	}

}