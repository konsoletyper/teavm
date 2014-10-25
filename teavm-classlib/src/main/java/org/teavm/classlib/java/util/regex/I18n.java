/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @author Nikolay A. Kuznetsov
 */

package org.teavm.classlib.java.util.regex;
import java.text.MessageFormat;

/**
 * Internationalization stub. All the messages in java.util.regexp
 * package done though this class. This class should be lately replaced with
 * real internationalization utility.
 *
 * @author Nikolay A. Kuznetsov
 *
 */
class I18n {
	public static String getMessage(String message) {
		return message;
	}

	public static String getFormattedMessage(String message, Object arg1) {
		return MessageFormat.format(message, new Object[] {arg1});
	}

	public static String getFormattedMessage(String message, Object arg1, Object arg2) {
		return MessageFormat.format(message, new Object[] {arg1, arg2});
	}

}
