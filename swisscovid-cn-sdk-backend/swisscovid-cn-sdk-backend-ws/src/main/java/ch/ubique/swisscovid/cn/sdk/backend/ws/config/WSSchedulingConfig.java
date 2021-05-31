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

import ch.ubique.swisscovid.cn.sdk.backend.data.InteractionDurationDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.SwissCovidDataService;
import ch.ubique.swisscovid.cn.sdk.backend.data.UUIDDataService;
import ch.ubique.swisscovid.cn.sdk.backend.ws.service.PhoneHeartbeatSilentPush;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

@Configuration
@EnableScheduling
public class WSSchedulingConfig implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WSSchedulingConfig.class);

    private final SwissCovidDataService swissCovidDataService;
    private final InteractionDurationDataService interactionDurationDataService;
    private final UUIDDataService uuidDataService;
    private final PhoneHeartbeatSilentPush phoneHeartbeatSilentPush;

    @Value("${db.cleanCron:0 0 3 * * ?}")
    private String cleanCron;

    @Value("${db.removeAfterDays:14}")
    private Integer removeAfterDays;

    @Value("${ws.heartBeatSilentPushCron}")
    private String heartBeatSilentPushCron;

    protected WSSchedulingConfig(
            final SwissCovidDataService swissCovidDataService,
            InteractionDurationDataService interactionDurationDataService,
            UUIDDataService uuidDataService,
            PhoneHeartbeatSilentPush phoneHeartbeatSilentPush) {
        this.swissCovidDataService = swissCovidDataService;
        this.interactionDurationDataService = interactionDurationDataService;
        this.uuidDataService = uuidDataService;
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
                                        "removing trace keys with end_time before: {}",
                                        removeBefore);
                                int removeCount =
                                        swissCovidDataService.removeTraceKeys(removeBefore);
                                logger.info("removed {} trace keys from db", removeCount);
                            } catch (Exception e) {
                                logger.error("Exception removing old trace keys", e);
                            }
                        },
                        new CronTrigger(cleanCron, TimeZone.getTimeZone("UTC"))));

        taskRegistrar.addCronTask(
                new CronTask(
                        () -> {
                            try {
                                logger.info("removing UUIDs older than {} days", removeAfterDays);
                                uuidDataService.cleanDB(Duration.ofDays(removeAfterDays));
                            } catch (Exception e) {
                                logger.error("Exception removing old UUIDs", e);
                            }
                        },
                        new CronTrigger(cleanCron, TimeZone.getTimeZone("UTC"))));

        taskRegistrar.addCronTask(
                new CronTask(
                        () -> {
                            try {
                                logger.info(
                                        "removing interaction duration entries older than {} days",
                                        removeAfterDays);
                                interactionDurationDataService.removeDurations(
                                        Duration.ofDays(removeAfterDays));
                            } catch (Exception e) {
                                logger.error(
                                        "Exception removing old interaction duration entries", e);
                            }
                        },
                        new CronTrigger(cleanCron, TimeZone.getTimeZone("UTC"))));

        taskRegistrar.addCronTask(
                new CronTask(
                        phoneHeartbeatSilentPush::sendHeartbeats,
                        new CronTrigger(heartBeatSilentPushCron, TimeZone.getTimeZone("UTC"))));
    }
}
