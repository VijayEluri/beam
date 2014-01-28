
import beampy
import numpy

jpy = beampy.jpy

Rectangle = jpy.get_class('java.awt.Rectangle')
Tile = jpy.get_class('org.esa.beam.framework.gpf.Tile')

import sys

#numpy.set_printoptions(threshold=10000*10000)

class MerisNdviTileComputer:

    def __init__(self):
        pass

    def initialize(self, operator):

        merisProduct = operator.getSourceProduct()

        width = merisProduct.getSceneRasterWidth()
        height = merisProduct.getSceneRasterHeight()

        self.b7 = merisProduct.getBand('radiance_7')
        self.b10 = merisProduct.getBand('radiance_10')

        ndviProduct = beampy.Product('pyNDVI', 'pyNDVI', width, height)
        ndviProduct.setPreferredTileSize(200, 200)
        #ndviProduct.setPreferredTileSize(width, height)
        self.ndviBand = ndviProduct.addBand('ndvi', beampy.ProductData.TYPE_FLOAT32)
        self.ndviFlagsBand = ndviProduct.addBand('ndvi_flags', beampy.ProductData.TYPE_UINT8)

        operator.setTargetProduct(ndviProduct)

    def computeTileStack(self, operator, targetTiles, targetRectangle):

        b7Tile = operator.getSourceTile(self.b7, targetRectangle)
        b10Tile = operator.getSourceTile(self.b10, targetRectangle)

        ndviTile = targetTiles.get(self.ndviBand)
        ndviFlagsTile = targetTiles.get(self.ndviFlagsBand)

        ndviTile = jpy.cast(ndviTile, Tile)
        ndviFlagsTile = jpy.cast(ndviFlagsTile, Tile)

        b7Data = b7Tile.getSamplesFloat()
        b10Data = b10Tile.getSamplesFloat()

        r7 = numpy.array(b7Data, dtype=numpy.float32)
        r10 = numpy.array(b10Data, dtype=numpy.float32)

        ndvi = (r10 - r7) / (r10 + r7)

        ndviLow = ndvi < 0.0
        ndviHigh = ndvi > 0.1
        ndviFlags = ndviLow + 2 * ndviHigh

        ndviTile.setSamples(ndvi)
        ndviFlagsTile.setSamples(ndviFlags)


    def dispose(self, operator):
        pass

    def dump(self, name, array):
        self.log.write(str(name) + ' = numpy.' + repr(array[0:100]))
        self.log.write('\n')
        self.log.flush()