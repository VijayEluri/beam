/*
 * $Id: ModisUint16BandReader.java,v 1.4 2007/04/17 10:03:50 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.obpg.bandreader;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.hdf.lib.HDF;
import org.esa.beam.framework.datamodel.ProductData;

public class ObpgUint16BandReader extends ObpgBandReader {

    private short[] _line;
    private int min;
    private int max;
    private short fill;
    private short[] targetData;
    private int targetIdx;

    public ObpgUint16BandReader(final int sdsId, final int layer, final boolean is3d) {
        super(sdsId, layer, is3d);
    }

    /**
     * Retrieves the data type of the band
     *
     * @return always {@link org.esa.beam.framework.datamodel.ProductData#TYPE_UINT16}
     */
    @Override
    public int getDataType() {
        return ProductData.TYPE_UINT16;
    }

    protected void prepareForReading(final int sourceOffsetX, final int sourceOffsetY, final int sourceWidth,
                                     final int sourceHeight, final int sourceStepX, final int sourceStepY,
                                     final ProductData destBuffer) {
        fill = (short) Math.round(_fillValue);
        if (_validRange == null) {
            min = 0;
            max = Short.MAX_VALUE * 2 + 1;
        } else {
            min = (int) Math.round(_validRange.getMin());
            max = (int) Math.round(_validRange.getMax());
        }
        targetData = (short[]) destBuffer.getElems();
        targetIdx = 0;
        ensureLineWidth(sourceWidth);
    }

    protected void readLine() throws HDFException {
        HDF.getInstance().SDreaddata(_sdsId, _start, _stride, _count, _line);
    }

    protected void validate(final int x) {
        final int value = _line[x] & 0xffff;
        if (value < min || value > max) {
            _line[x] = fill;
        }
    }

    protected void assign(final int x) {
        targetData[targetIdx++] = _line[x];
    }

    private void ensureLineWidth(final int sourceWidth) {
        if ((_line == null) || (_line.length != sourceWidth)) {
            _line = new short[sourceWidth];
        }
    }
}