package io.inprice.api.app.webhooks;

import com.stripe.model.Event;
import com.stripe.net.Webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.Props;
import io.inprice.api.framework.Controller;
import io.inprice.api.framework.Router;
import io.inprice.api.helpers.Commons;
import io.inprice.common.helpers.Beans;
import io.javalin.Javalin;

@Router
public class StripeController implements Controller {

  private static final Logger log = LoggerFactory.getLogger(StripeController.class);

  private final StripeService service = Beans.getSingleton(StripeService.class);

  @Override
  public void addRoutes(Javalin app) {

    app.post(Consts.Paths.Webhook.STRIPE, (ctx) -> {
      final String payload = ctx.body();
      final String sigHeader = ctx.req.getHeader("Stripe-Signature");
      try {
        Event event = Webhook.constructEvent(
          payload, sigHeader, Props.API_KEYS_STRIPE_WEBHOOK()
        );
        ctx.json(Commons.createResponse(ctx, service.handle(event)));
      } catch (Exception e) {
        log.error("An error occurred", e);
        ctx.json(Commons.createResponse(ctx, Responses.BAD_REQUEST));
      }
    });

  }

}