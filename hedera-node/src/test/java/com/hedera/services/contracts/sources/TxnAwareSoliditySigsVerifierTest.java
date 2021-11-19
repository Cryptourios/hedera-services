package com.hedera.services.contracts.sources;

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

import com.hedera.services.context.TransactionContext;
import com.hedera.services.keys.SyncActivationCheck;
import com.hedera.services.legacy.core.jproto.JKey;
import com.hedera.services.sigs.verification.SyncVerifier;
import com.hedera.services.state.merkle.MerkleAccount;
import com.hedera.services.utils.EntityNum;
import com.hedera.services.utils.PlatformTxnAccessor;
import com.hedera.test.factories.scenarios.TxnHandlingScenario;
import com.hedera.test.utils.IdUtils;
import com.hederahashgraph.api.proto.java.AccountID;
import com.swirlds.merkle.map.MerkleMap;
import org.hyperledger.besu.datatypes.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.hedera.services.utils.EntityIdUtils.asTypedSolidityAddress;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.never;

class TxnAwareSoliditySigsVerifierTest {
	private static final Address PRETEND_RECIPIENT_ADDR = Address.ALTBN128_ADD;
	private static final Address PRETEND_CONTRACT_ADDR = Address.ALTBN128_MUL;

	AccountID payer = IdUtils.asAccount("0.0.2");
	AccountID sigRequired = IdUtils.asAccount("0.0.555");
	AccountID smartContract = IdUtils.asAccount("0.0.666");
	AccountID noSigRequired = IdUtils.asAccount("0.0.777");
	Set<AccountID> touched;
	SyncVerifier syncVerifier;
	PlatformTxnAccessor accessor;
	JKey expectedKey;

	MerkleAccount sigReqAccount, noSigReqAccount, contract;

	TransactionContext txnCtx;
	SyncActivationCheck areActive;
	MerkleMap<EntityNum, MerkleAccount> accounts;

	TxnAwareSoliditySigsVerifier subject;

	@BeforeEach
	private void setup() throws Exception {
		syncVerifier = mock(SyncVerifier.class);
		expectedKey = TxnHandlingScenario.MISC_ACCOUNT_KT.asJKey();

		contract = mock(MerkleAccount.class);
		given(contract.isSmartContract()).willReturn(true);
		given(contract.isReceiverSigRequired()).willReturn(true);
		sigReqAccount = mock(MerkleAccount.class);
		given(sigReqAccount.isReceiverSigRequired()).willReturn(true);
		given(sigReqAccount.getAccountKey()).willReturn(expectedKey);
		noSigReqAccount = mock(MerkleAccount.class);
		given(noSigReqAccount.isReceiverSigRequired()).willReturn(false);

		accessor = mock(PlatformTxnAccessor.class);
		txnCtx = mock(TransactionContext.class);
		given(txnCtx.accessor()).willReturn(accessor);
		given(txnCtx.activePayer()).willReturn(payer);

		accounts = mock(MerkleMap.class);
		given(accounts.get(EntityNum.fromAccountId(payer))).willReturn(sigReqAccount);
		given(accounts.get(EntityNum.fromAccountId(sigRequired))).willReturn(sigReqAccount);
		given(accounts.get(EntityNum.fromAccountId(noSigRequired))).willReturn(noSigReqAccount);
		given(accounts.get(EntityNum.fromAccountId(smartContract))).willReturn(contract);

		areActive = mock(SyncActivationCheck.class);

		subject = new TxnAwareSoliditySigsVerifier(syncVerifier, txnCtx, areActive, () -> accounts);
	}

	@Test
	void filtersContracts() {
		boolean contractFlag = subject.hasActiveKeyOrNoReceiverSigReq(
				asTypedSolidityAddress(smartContract), PRETEND_RECIPIENT_ADDR, PRETEND_CONTRACT_ADDR);

		assertTrue(contractFlag);
		verify(areActive, never()).allKeysAreActive(
				any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void filtersNoSigRequired() {
		given(areActive.allKeysAreActive(
				argThat(List.of(expectedKey)::equals),
				any(), any(), any(), any(), any(), any(), any())).willReturn(true);

		boolean sigRequiredFlag = subject.hasActiveKeyOrNoReceiverSigReq(
				asTypedSolidityAddress(sigRequired), PRETEND_RECIPIENT_ADDR, PRETEND_CONTRACT_ADDR);

		assertTrue(sigRequiredFlag);
		verify(areActive).allKeysAreActive(any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void filtersPayerSinceSigIsGuaranteed() {
		boolean payerFlag = subject.hasActiveKeyOrNoReceiverSigReq(
				asTypedSolidityAddress(payer), PRETEND_RECIPIENT_ADDR, PRETEND_CONTRACT_ADDR);

		assertTrue(payerFlag);
		verify(areActive, never()).allKeysAreActive(
				any(), any(), any(), any(), any(), any(), any(), any());
	}
}
