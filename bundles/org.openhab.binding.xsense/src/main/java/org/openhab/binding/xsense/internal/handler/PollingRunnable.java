/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.xsense.internal.handler;

import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link PollingRunnable} takes care for polling device states
 *
 * @author Jakob Fellner - Initial contribution
 */
@NonNullByDefault
abstract public class PollingRunnable implements Runnable {
    final ReentrantLock pollingLock = new ReentrantLock();

    @Override
    public void run() {
        try {
            pollingLock.lock();
            doConnectedRun();
        } finally {
            pollingLock.unlock();
        }
    }

    protected abstract void doConnectedRun();
}
