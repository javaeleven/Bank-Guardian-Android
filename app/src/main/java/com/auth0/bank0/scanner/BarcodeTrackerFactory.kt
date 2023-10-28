/*
 * Copyright (c) 2016 Auth0 (http://auth0.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.auth0.bank0.scanner

import com.auth0.bank0.scanner.camera.GraphicOverlay
import com.auth0.bank0.scanner.utils.Barcode
import com.auth0.bank0.scanner.utils.MultiProcessor
import com.auth0.bank0.scanner.utils.Tracker

/**
 * Factory for creating a tracker and associated graphic to be associated with a new barcode.  The
 * multi-processor uses this factory to create barcode trackers as needed -- one for each barcode.
 */
internal class BarcodeTrackerFactory(
        private val barcodeGraphicOverlay: GraphicOverlay<BarcodeGraphic>,
        private val listener: BarcodeTrackerListener
) : MultiProcessor.Factory<Barcode> {
    override fun create(barcode: Barcode): Tracker<Barcode> {
        listener.onBarcodeDetected(barcode)
        val graphic = BarcodeGraphic(barcodeGraphicOverlay)
        return BarcodeGraphicTracker(barcodeGraphicOverlay, graphic)
    }
}