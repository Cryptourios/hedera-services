package com.hedera.services.contracts.execution;

/*
 * -
 * ‌
 * Hedera Services Node
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 */

import org.apache.tuweni.units.bigints.UInt256;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class HederaBlockValuesTest {
    HederaBlockValues subject;

    @Test
    void instancing() {
        final var gasLimit = 1L;
        final var timestamp = 2L;

        subject = new HederaBlockValues(gasLimit, timestamp);
        Assertions.assertEquals(gasLimit, subject.getGasLimit());
        Assertions.assertEquals(timestamp, subject.getTimestamp());
        Assertions.assertEquals(Optional.of(0L), subject.getBaseFee());
        Assertions.assertEquals(UInt256.ZERO, subject.getDifficultyBytes());
        Assertions.assertEquals(timestamp, subject.getNumber());
    }
}
