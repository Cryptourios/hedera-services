package com.hederahashgraph.fee;

/*-
 * ‌
 * Hedera Services API Utilities
 * ​
 * Copyright (C) 2018 - 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.ConsensusCreateTopicTransactionBody;
import com.hederahashgraph.api.proto.java.ConsensusDeleteTopicTransactionBody;
import com.hederahashgraph.api.proto.java.ConsensusUpdateTopicTransactionBody;
import com.hederahashgraph.api.proto.java.Duration;
import com.hederahashgraph.api.proto.java.FeeComponents;
import com.hederahashgraph.api.proto.java.FeeData;
import com.hederahashgraph.api.proto.java.Key;
import com.hederahashgraph.api.proto.java.KeyList;
import com.hederahashgraph.api.proto.java.Timestamp;
import com.hederahashgraph.api.proto.java.TopicID;
import com.hederahashgraph.api.proto.java.TransactionBody;
import com.hederahashgraph.exception.InvalidTxBodyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsensusServiceFeeBuilderTest {
    private static final Key A_KEY = Key.newBuilder()
            .setEd25519(ByteString.copyFromUtf8("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
            .build();
    private static final String MEMO = "This is a memo.";
    private static final Timestamp TIMESTAMP = Timestamp.newBuilder().setSeconds(100L).setNanos(100).build();
    private static final AccountID ACCOUNT_A = AccountID.newBuilder().setAccountNum(3L).setRealmNum(0L)
            .setShardNum(0L).build();
    private static final Duration DURATION = Duration.newBuilder().setSeconds(1000L).build();
    private static final SigValueObj SIG_VALUE_OBJ = new SigValueObj(1, 1, 1);

    @Test
    void builderMethodsThrowException() {
        final var txnBody = TransactionBody.newBuilder().build();

        assertThrows(InvalidTxBodyException.class,
                () -> ConsensusServiceFeeBuilder.getConsensusCreateTopicFee(null, null));
        assertThrows(InvalidTxBodyException.class,
                () -> ConsensusServiceFeeBuilder.getConsensusCreateTopicFee(txnBody, null));
        assertThrows(InvalidTxBodyException.class,
                () -> ConsensusServiceFeeBuilder.getConsensusUpdateTopicFee(null, 100L, null));
        assertThrows(InvalidTxBodyException.class,
                () -> ConsensusServiceFeeBuilder.getConsensusUpdateTopicFee(txnBody, 100L, null));
        assertThrows(InvalidTxBodyException.class,
                () -> ConsensusServiceFeeBuilder.getConsensusDeleteTopicFee(null, null));
        assertThrows(InvalidTxBodyException.class,
                () -> ConsensusServiceFeeBuilder.getConsensusDeleteTopicFee(txnBody, null));
    }

    @Test
    void getConsensusCreateTopicFeeHappyPath() throws InvalidTxBodyException {
        final var txnBodyA = TransactionBody.newBuilder()
                .setConsensusCreateTopic(ConsensusCreateTopicTransactionBody.newBuilder()
                        .setAdminKey(A_KEY)
                        .setSubmitKey(A_KEY)
                        .setMemo(MEMO)
                        .setAutoRenewAccount(ACCOUNT_A)
                        .setAutoRenewPeriod(DURATION)
                        .build())
                .build();
        final var txnBodyB = TransactionBody.newBuilder()
                .setConsensusCreateTopic(ConsensusCreateTopicTransactionBody.newBuilder()
                        .setAdminKey(A_KEY)
                        .setSubmitKey(A_KEY)
                        .setMemo(MEMO)
                        .setAutoRenewAccount(ACCOUNT_A)
                        .build())
                .build();

        final var expectedA = getFeeData(1L, 188L, 1L, 4L, 3L, 62L);
        final var expectedB = getFeeData(1L, 188L, 1L, 4L, 3L, 6L);
        final var actualA = ConsensusServiceFeeBuilder.getConsensusCreateTopicFee(txnBodyA, SIG_VALUE_OBJ);
        final var actualB = ConsensusServiceFeeBuilder.getConsensusCreateTopicFee(txnBodyB, SIG_VALUE_OBJ);

        assertEquals(expectedA, actualA);
        assertEquals(expectedB, actualB);
    }

    @Test
    void getConsensusUpdateTopicFeeHappyPath() throws InvalidTxBodyException {
        final var txnBodyA = TransactionBody.newBuilder()
                .setConsensusUpdateTopic(ConsensusUpdateTopicTransactionBody.newBuilder()
                        .setMemo(StringValue.of(MEMO))
                        .setAdminKey(A_KEY)
                        .setExpirationTime(TIMESTAMP)
                        .setAutoRenewPeriod(DURATION)
                        .build())
                .build();
        final var txnBodyB = TransactionBody.newBuilder()
                .setConsensusUpdateTopic(ConsensusUpdateTopicTransactionBody.newBuilder()
                        .setMemo(StringValue.of(MEMO))
                        .setAdminKey(A_KEY)
                        .build())
                .build();

        final var expectedA = getFeeData(1L, 164L, 1L, 4L, 1L, 6L);
        final var expectedB = getFeeData(1L, 148L, 1L, 4L, 1L, 6L);
        final var actualA = ConsensusServiceFeeBuilder.getConsensusUpdateTopicFee(txnBodyA,
                100L, SIG_VALUE_OBJ);
        final var actualB = ConsensusServiceFeeBuilder.getConsensusUpdateTopicFee(txnBodyB,
                100L, SIG_VALUE_OBJ);

        assertEquals(expectedA, actualA);
        assertEquals(expectedB, actualB);
    }

    @Test
    void getUpdateTopicRbsIncreaseHappyPath() {
        final var bKey = Key.newBuilder()
                .setEd25519(ByteString.copyFromUtf8("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
                .setKeyList(KeyList.newBuilder().build())
                .build();
        final var emptyAccount = AccountID.newBuilder().setAccountNum(0L).setRealmNum(0L)
                .setShardNum(0L).build();
        final var txnBodyA = ConsensusUpdateTopicTransactionBody.newBuilder()
                .setMemo(StringValue.of(MEMO))
                .setAdminKey(bKey)
                .setSubmitKey(bKey)
                .setAutoRenewPeriod(DURATION)
                .setAutoRenewAccount(emptyAccount)
                .setExpirationTime(TIMESTAMP)
                .build();
        final var txnBodyB = ConsensusUpdateTopicTransactionBody.newBuilder()
                .setAutoRenewPeriod(DURATION)
                .build();


        final var actualA = ConsensusServiceFeeBuilder.getUpdateTopicRbsIncrease(TIMESTAMP, A_KEY, A_KEY,
                MEMO, true, TIMESTAMP, txnBodyA);
        final var actualB = ConsensusServiceFeeBuilder.getUpdateTopicRbsIncrease(TIMESTAMP, A_KEY, A_KEY,
                MEMO, true, TIMESTAMP, txnBodyB);

        assertEquals(0L, actualA);
        assertEquals(0L, actualB);
    }

    @Test
    void getConsensusDeleteTopicFeeHappyPath() throws InvalidTxBodyException {
        final var topicId = TopicID.newBuilder().setTopicNum(5L).setRealmNum(0L).setShardNum(0L).build();
        final var txnBody = TransactionBody.newBuilder()
                .setConsensusDeleteTopic(ConsensusDeleteTopicTransactionBody.newBuilder()
                        .setTopicID(topicId)
                        .build())
                .build();

        final var expected = getFeeData(1L, 101L, 1L, 4L, 1L, 6L);
        final var actual = ConsensusServiceFeeBuilder.getConsensusDeleteTopicFee(txnBody, SIG_VALUE_OBJ);

        assertEquals(expected, actual);
    }

    @Test
    void helperMethodsWork() {
        final var baseTopicRamByteSize = 100;

        assertEquals(baseTopicRamByteSize, ConsensusServiceFeeBuilder.getTopicRamBytes(0));
        assertEquals(baseTopicRamByteSize + 20, ConsensusServiceFeeBuilder.getTopicRamBytes(20));

        final var actualWithAutoRenewAccountAndMemo = ConsensusServiceFeeBuilder.computeVariableSizedFieldsUsage(
                A_KEY, A_KEY, MEMO, true);
        final var actualWithoutAutRenewAccountAndMemo = ConsensusServiceFeeBuilder.computeVariableSizedFieldsUsage(
                A_KEY, A_KEY, null, false);

        assertEquals(103, actualWithAutoRenewAccountAndMemo);
        assertEquals(64, actualWithoutAutRenewAccountAndMemo);
    }

    private static final FeeData getFeeData(final long constant, final long bpt, final long vpt,
                                      final long bpr, final long rbhNetwork, final long rbhService) {
        return FeeData.newBuilder()
                .setNodedata(FeeComponents.newBuilder()
                        .setConstant(constant)
                        .setBpt(bpt)
                        .setVpt(vpt)
                        .setBpr(bpr)
                        .build())
                .setNetworkdata(FeeComponents.newBuilder()
                        .setConstant(constant)
                        .setBpt(bpt)
                        .setVpt(vpt)
                        .setRbh(rbhNetwork)
                        .build())
                .setServicedata(FeeComponents.newBuilder()
                        .setConstant(constant)
                        .setRbh(rbhService)
                        .build())
                .build();
    }
}
