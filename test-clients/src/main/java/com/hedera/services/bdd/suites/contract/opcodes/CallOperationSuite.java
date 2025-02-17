package com.hedera.services.bdd.suites.contract.opcodes;

/*-
 * ‌
 * Hedera Services Test Clients
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

import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.infrastructure.meta.ContractResources;
import com.hedera.services.bdd.suites.HapiApiSuite;
import com.hederahashgraph.api.proto.java.AccountID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.contractCallLocal;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getAccountBalance;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCall;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.contractCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileCreate;
import static com.hedera.services.bdd.spec.utilops.CustomSpecAssert.allRunFor;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.withOpContext;
import static com.hedera.services.legacy.core.CommonUtils.calculateSolidityAddress;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_SOLIDITY_ADDRESS;

public class CallOperationSuite extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(CallOperationSuite.class);

	public static void main(String... args) {
		new CallOperationSuite().runSuiteAsync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(new HapiApiSpec[]{
				callingContract(),
				verifiesExistence(),
		});
	}

	HapiApiSpec verifiesExistence() {
		final String CONTRACT = "callOpChecker";
		final String INVALID_ADDRESS = "0x0000000000000000000000000000000000123456";
		final String ACCOUNT = "account";
		final int EXPECTED_BALANCE = 10;

		return defaultHapiSpec("VerifiesExistence")
				.given(
						fileCreate("bytecode").path(ContractResources.CALL_OPERATIONS_CHECKER),
						contractCreate(CONTRACT)
								.bytecode("bytecode")
								.gas(1_000_000),
						cryptoCreate(ACCOUNT).balance(0L)
				).when(
				).then(
						contractCall(CONTRACT,
								ContractResources.CALL_OP_CHECKER_ABI,
								INVALID_ADDRESS)
								.hasKnownStatus(INVALID_SOLIDITY_ADDRESS),
						withOpContext((spec, opLog) -> {
							AccountID id = spec.registry().getAccountID(ACCOUNT);
							String solidityAddress = calculateSolidityAddress((int) id.getShardNum(), id.getRealmNum(), id.getAccountNum());

							final var contractCall = contractCall(CONTRACT,
									ContractResources.CALL_OP_CHECKER_ABI,
									solidityAddress)
									.sending(EXPECTED_BALANCE);

							final var balance = getAccountBalance(ACCOUNT).hasTinyBars(EXPECTED_BALANCE);

							allRunFor(spec, contractCall, balance);
						})
				);
	}

	HapiApiSpec callingContract() {
		final String CONTRACT = "callingContract";
		final String INVALID_ADDRESS = "0x0000000000000000000000000000000000123456";
		return defaultHapiSpec("CallingContract")
				.given(
						fileCreate("callingContractBytecode").path(ContractResources.CALLING_CONTRACT)
				).when(
						contractCreate(CONTRACT).bytecode("callingContractBytecode"),
						contractCall(CONTRACT, ContractResources.CALLING_CONTRACT_SET_VALUE, 35),
						contractCallLocal("callingContract",
								ContractResources.CALLING_CONTRACT_VIEW_VAR)
								.logged(),
						contractCall(CONTRACT, ContractResources.CALLING_CONTRACT_CALL_CONTRACT,
								INVALID_ADDRESS, 222)
								.hasKnownStatus(INVALID_SOLIDITY_ADDRESS)
				).then(
						contractCallLocal(CONTRACT,
								ContractResources.CALLING_CONTRACT_VIEW_VAR)
								.logged()
				);
	}


	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}
