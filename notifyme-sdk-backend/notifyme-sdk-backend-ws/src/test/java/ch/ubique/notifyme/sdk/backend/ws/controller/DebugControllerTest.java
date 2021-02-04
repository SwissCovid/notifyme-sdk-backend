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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.ubique.notifyme.sdk.backend.model.ProblematicDiaryEntryWrapperOuterClass.ProblematicDiaryEntryWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "enable-debug"})
public class DebugControllerTest extends BaseControllerTest {
    private String diaryEntriesEndPoint;

    @Before
    public void setUp() {
        final String debugControllerEndPoint = "/v1/debug";
        diaryEntriesEndPoint = debugControllerEndPoint + "/diaryEntries";
    }

    @Test
    public void uploadDiaryEntryProtobufShouldReturnOk() throws Exception {
        final ProblematicDiaryEntryWrapper wrapper =
                DebugControllerTestHelper.getTestProblematicDiaryEntryWrapper();

        mockMvc.perform(
                        post(diaryEntriesEndPoint)
                                .contentType("application/x-protobuf")
                                .content(wrapper.toByteArray()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
