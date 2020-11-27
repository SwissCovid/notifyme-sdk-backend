/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.ws.controller;

import ch.ubique.notifyme.sdk.backend.data.NotifyMeDataService;
import ch.ubique.notifyme.sdk.backend.model.tracekey.TraceKey;
import ch.ubique.notifyme.sdk.backend.model.util.DateUtil;
import ch.ubique.notifyme.sdk.backend.ws.SodiumWrapper;
import ch.ubique.openapi.docannotations.Documentation;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/v1/debug")
@CrossOrigin(origins = {"https://upload-dev.notify-me.ch", "https://upload.notify-me.ch"})
public class DebugController {
    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    private final NotifyMeDataService dataService;
    private final SodiumWrapper sodiumWrapper;

    public DebugController(NotifyMeDataService dataService, SodiumWrapper sodiumWrapper) {
        this.dataService = dataService;
        this.sodiumWrapper = sodiumWrapper;
    }

    @GetMapping(value = "")
    @Documentation(
            description = "Hello return",
            responses = {"200=>server live"})
    public @ResponseBody ResponseEntity<String> hello() {
        return ResponseEntity.ok()
                .header("X-HELLO", "notifyme")
                .body("Hello from NotifyMe Debug WS v1");
    }

    @PostMapping(value = "/traceKey")
    public @ResponseBody ResponseEntity<String> uploadTraceKey(
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @RequestParam @Documentation(description = "url base64 encoded encrypted secret key")
                    String ctx,
            @RequestParam String message)
            throws UnsupportedEncodingException {
        TraceKey traceKey = new TraceKey();
        traceKey.setStartTime(DateUtil.toInstant(startTime));
        traceKey.setEndTime(DateUtil.toInstant(endTime));
        try {
            byte[] secretKey =
                    sodiumWrapper.decryptQrTrace(
                            Base64.getUrlDecoder().decode(ctx.getBytes("UTF-8")));
            traceKey.setSecretKey(secretKey);
            byte[] nonce = sodiumWrapper.createNonceForMessageEncytion();
            byte[] encryptedMessage =
                    sodiumWrapper.encryptMessage(traceKey.getSecretKey(), nonce, message);
            traceKey.setMessage(encryptedMessage);
            traceKey.setNonce(nonce);
        } catch (InvalidProtocolBufferException e) {
            logger.error("unable to parse decrypted ctx protobuf", e);
        }

        dataService.insertTraceKey(traceKey);
        return ResponseEntity.ok().body("OK");
    }
}
