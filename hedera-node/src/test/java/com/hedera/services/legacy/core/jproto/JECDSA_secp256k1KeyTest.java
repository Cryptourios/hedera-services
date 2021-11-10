package com.hedera.services.legacy.core.jproto;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JECDSA_secp256k1KeyTest {
	JECDSA_secp256k1Key subject;
	byte[] bytes;

	@BeforeEach
	void setUp() {
		bytes = new byte[33];
		bytes[0] = 0x03;
		subject = new JECDSA_secp256k1Key(bytes);
	}

	@Test
	void emptyJECDSA_secp256k1KeyTest() {
		JECDSA_secp256k1Key key1 = new JECDSA_secp256k1Key(null);
		assertTrue(key1.isEmpty());
		assertFalse(key1.isValid());

		JECDSA_secp256k1Key key2 = new JECDSA_secp256k1Key(new byte[0]);
		assertTrue(key2.isEmpty());
		assertFalse(key2.isValid());
	}

	@Test
	void nonEmptyInvalidLengthJECDSA_secp256k1KeyTest() {
		JECDSA_secp256k1Key key = new JECDSA_secp256k1Key(new byte[1]);
		assertFalse(key.isEmpty());
		assertFalse(key.isValid());
	}

	@Test
	void nonEmptyValid0x02JECDSA_secp256k1KeyTest() {
		byte[] bytes = new byte[33];
		bytes[0] = 0x02;
		JECDSA_secp256k1Key key = new JECDSA_secp256k1Key(bytes);
		assertFalse(key.isEmpty());
		assertTrue(key.isValid());
	}

	@Test
	void nonEmptyValid0x03JECDSA_secp256k1KeyTest() {
		assertFalse(subject.isEmpty());
		assertTrue(subject.isValid());
	}

	@Test
	void getterWorks() {
		assertEquals(bytes, subject.getECDSAsecp256k1Key());
	}

	@Test
	void toStringWorks() {
		assertEquals(
				"<JECDSA_secp256k1Key: ECDSA_secp256k1Key " +
						"hex=030000000000000000000000000000000000000000000000000000000000000000>",
				subject.toString());
	}
}
