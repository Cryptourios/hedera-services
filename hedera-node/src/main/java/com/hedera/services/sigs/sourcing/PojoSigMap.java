package com.hedera.services.sigs.sourcing;

/*-
 * ‌
 * Hedera Services Node
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

import com.hederahashgraph.api.proto.java.SignatureMap;

public class PojoSigMap {
	private static final int PUB_KEY_PREFIX_INDEX = 0;
	private static final int SIG_BYTES_INDEX = 1;
	private static final int DATA_PER_SIG_PAIR = 2;

	private enum KeyType {
		ED25519(32), ECDSA_SECP256K1(33);

		private final int length;

		KeyType(final int length) {
			this.length = length;
		}
	}

	private final boolean usesEcdsaSecp256k1;
	private final KeyType[] keyTypes;
	private final byte[][][] rawMap;

	private PojoSigMap(final byte[][][] rawMap, final KeyType[] keyTypes, final boolean usesEcdsaSecp256k1) {
		this.rawMap = rawMap;
		this.keyTypes = keyTypes;
		this.usesEcdsaSecp256k1 = usesEcdsaSecp256k1;
	}

	public static PojoSigMap fromGrpc(final SignatureMap sigMap) {
		final var n = sigMap.getSigPairCount();
		final var rawMap = new byte[n][DATA_PER_SIG_PAIR][];
		final var keyTypes = new KeyType[n];
		var usesEcdsaSecp256k1 = false;
		for (var i = 0; i < n; i++) {
			final var sigPair = sigMap.getSigPair(i);
			rawMap[i][PUB_KEY_PREFIX_INDEX] = sigPair.getPubKeyPrefix().toByteArray();
			if (!sigPair.getECDSASecp256K1().isEmpty()) {
				rawMap[i][SIG_BYTES_INDEX] = sigPair.getECDSASecp256K1().toByteArray();
				keyTypes[i] = KeyType.ECDSA_SECP256K1;
				usesEcdsaSecp256k1 = true;
			} else {
				rawMap[i][SIG_BYTES_INDEX] = sigPair.getEd25519().toByteArray();
				keyTypes[i] = KeyType.ED25519;
			}
		}
		return new PojoSigMap(rawMap, keyTypes, usesEcdsaSecp256k1);
	}

	public boolean isFullPrefixAt(final int i) {
		if (i < 0 || i >= rawMap.length) {
			throw new IllegalArgumentException("Requested prefix at index " + i + ", not in [0, " + rawMap.length + ")");
		}
		return keyTypes[i].length == rawMap[i][PUB_KEY_PREFIX_INDEX].length;
	}

	public boolean usesEcdsaSecp256k1() {
		return usesEcdsaSecp256k1;
	}

	public byte[] pubKeyPrefix(int i) {
		return rawMap[i][PUB_KEY_PREFIX_INDEX];
	}

	public byte[] primitiveSignature(int i) {
		return rawMap[i][SIG_BYTES_INDEX];
	}

	public int numSigsPairs() {
		return rawMap.length;
	}
}
