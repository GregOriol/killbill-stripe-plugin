/*
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.stripe;

import java.util.Map;

import javax.annotation.Nullable;

import org.killbill.billing.osgi.api.Healthcheck;
import org.killbill.billing.tenant.api.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stripe.exception.StripeException;
import com.stripe.model.Balance;
import com.stripe.net.RequestOptions;

public class StripeHealthcheck implements Healthcheck {

    private static final Logger logger = LoggerFactory.getLogger(StripeHealthcheck.class);

    private final StripeConfigPropertiesConfigurationHandler stripeConfigPropertiesConfigurationHandler;

    public StripeHealthcheck(final StripeConfigPropertiesConfigurationHandler stripeConfigPropertiesConfigurationHandler) {
        this.stripeConfigPropertiesConfigurationHandler = stripeConfigPropertiesConfigurationHandler;
    }

    @Override
    public HealthStatus getHealthStatus(@Nullable final Tenant tenant, @Nullable final Map properties) {
        if (tenant == null) {
            // The plugin is running
            return HealthStatus.healthy("Stripe OK");
        } else {
            // Specifying the tenant lets you also validate the tenant configuration
            final StripeConfigProperties stripeConfigProperties = stripeConfigPropertiesConfigurationHandler.getConfigurable(tenant.getId());
            return pingStripe(stripeConfigProperties);
        }
    }

    private HealthStatus pingStripe(final StripeConfigProperties stripeConfigProperties) {
        final RequestOptions requestOptions = RequestOptions.builder()
                                                            .setConnectTimeout(Integer.parseInt(stripeConfigProperties.getConnectionTimeout()))
                                                            .setReadTimeout(Integer.parseInt(stripeConfigProperties.getReadTimeout()))
                                                            .setApiKey(stripeConfigProperties.getApiKey())
                                                            .build();

        try {
            Balance.retrieve(requestOptions);
            return HealthStatus.healthy("Stripe OK");
        } catch (final StripeException e) {
            logger.warn("Healthcheck error", e);
            return HealthStatus.unHealthy("Stripe error: " + e.getMessage());
        }
    }
}
