/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.ubique.notifyme.sdk.backend.ws.controller.web;

import ch.ubique.notifyme.sdk.backend.data.DiaryEntryDataService;
import ch.ubique.notifyme.sdk.backend.model.event.CriticalEvent;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("")
public class WebCriticalEventController {

    private final DiaryEntryDataService diaryEntryDataService;

    public WebCriticalEventController(final DiaryEntryDataService diaryEntryDataService) {
        this.diaryEntryDataService = diaryEntryDataService;
    }

    @GetMapping("/criticalevent")
    public String criticalEvent(final Model model) {
        final List<CriticalEvent> criticalEvents = diaryEntryDataService.getCriticalEvents();
        model.addAttribute("criticalEvents", criticalEvents);
        return "criticalEvent";
    }
}
