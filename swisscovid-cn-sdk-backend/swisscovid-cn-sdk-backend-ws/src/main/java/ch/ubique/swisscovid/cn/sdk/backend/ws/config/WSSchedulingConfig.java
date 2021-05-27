/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.swisscovid.cn.sdk.backend.ws.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataServiceV3;
import ch.ubique.swisscovid.cn.sdk.backend.ws.service.IOSHeartbeatSilentPush;

@Configuration
@EnableScheduling
public class WSSchedulingConfig implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WSSchedulingConfig.class);

    private final SwissCovidDataServiceV3 swissCovidDataServiceV3;
    private final IOSHeartbeatSilentPush phoneHeartbeatSilentPush;

    @Value("${db.cleanCron:0 0 * * * ?}")
    private String cleanCron;

    @Value("${db.removeAfterDays:14}")
    private Integer removeAfterDays;

    @Value("${ws.heartBeatSilentPushCron}")
    private String heartBeatSilentPushCron;

    protected WSSchedulingConfig(
            final SwissCovidDataServiceV3 swissCovidDataServiceV3,
            @Nullable IOSHeartbeatSilentPush phoneHeartbeatSilentPush) {
        this.swissCovidDataServiceV3 = swissCovidDataServiceV3;
        this.phoneHeartbeatSilentPush = phoneHeartbeatSilentPush;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addCronTask(
                new CronTask(
                        () -> {
                            try {
                                Instant removeBefore =
                                        Instant.now().minus(removeAfterDays, ChronoUnit.DAYS);
                                logger.info(
                                        "removing trace keys v3 with end_time before: {}",
                                        removeBefore);
                                int removeCount =
                                        swissCovidDataServiceV3.removeTraceKeys(removeBefore);
                                logger.info("removed {} trace keys v3 from db", removeCount);
                            } catch (Exception e) {
                                logger.error("Exception removing old trace keys v3", e);
                            }
                        },
                        new CronTrigger(cleanCron, TimeZone.getTimeZone("UTC"))));

        // push is optional, only trigger if set
        if (phoneHeartbeatSilentPush != null) {
	        taskRegistrar.addCronTask(
	                new CronTask(
	                        phoneHeartbeatSilentPush::sendHeartbeats,
	                        new CronTrigger(heartBeatSilentPushCron, TimeZone.getTimeZone("UTC"))));
        }
    }
}
