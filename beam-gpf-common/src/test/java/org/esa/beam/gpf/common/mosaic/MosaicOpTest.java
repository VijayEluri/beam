package org.esa.beam.gpf.common.mosaic;

import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class MosaicOpTest {

    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;


    private static Product product1;
    private static Product product2;
    private static Product product3;

    @BeforeClass
    public static void setup() throws FactoryException, TransformException {
        product1 = createProduct("P1", 0, 0, 2.0f);
        product2 = createProduct("P2", 4, -4, 3.0f);
        product3 = createProduct("P3", -5, 5, 5.0f);
        // We have to load SPIs manually, otherwise SPI for Reproject is not available
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.loadOperatorSpis();
    }

    @AfterClass
    public static void teardown() {
        product1.dispose();
        product2.dispose();
        product3.dispose();
    }

    @Test
    public void testMosaickingSimple() throws IOException {
        final MosaicOp op = new MosaicOp();
        op.setSourceProducts(new Product[]{product1, product2, product3});
        op.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1"),

        };
        op.bounds = new MosaicOp.GeoBounds(-10, 10, 10, -10);
        op.pixelSizeX = 1;
        op.pixelSizeY = 1;

        final Product product = op.getTargetProduct();

        final GeoPos[] geoPositions = {
                new GeoPos(8, -8), new GeoPos(4, -4), new GeoPos(-1, 1), new GeoPos(-4, 4), new GeoPos(-8, 8)
        };

        Band b1Band = product.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 5.0f, 3.5f, 3.333333f, 2.5f});

        Band countBand = product.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 1, 2, 3, 2});
    }

    @Test
    public void testMosaickingWithConditions() {
        final MosaicOp op = new MosaicOp();
        op.setSourceProducts(new Product[]{product1, product2, product3});
        op.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1")
        };
        op.conditions = new MosaicOp.Condition[]{
                new MosaicOp.Condition("b1_cond", "b1 != 3", true)
        };
        op.bounds = new MosaicOp.GeoBounds(-10, 10, 10, -10);
        op.pixelSizeX = 1;
        op.pixelSizeY = 1;

        final Product product = op.getTargetProduct();

        final GeoPos[] geoPositions = {
                new GeoPos(8, -8), new GeoPos(4, -4), new GeoPos(-1, 1), new GeoPos(-4, 4), new GeoPos(-8, 8)
        };

        Band b1Band = product.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 5.0f, 3.5f, 3.5f, 2.0f});

        Band countBand = product.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 1, 2, 2, 1});

        Band condBand = product.getBand("b1_cond");
        assertSampleValuesInt(condBand, geoPositions, new int[]{0, 1, 2, 2, 1});
    }

    @Test
    public void testMosaickingWithInvalidSourceSamples() throws IOException {
        final Product product1Copy = ProductSubsetBuilder.createProductSubset(product1, null, "P1", "Descr");
        final Band flagBand = product1Copy.addBand("flag", ProductData.TYPE_INT32);
        final BufferedImage flagImage = new BufferedImage(WIDTH, HEIGHT, DataBuffer.TYPE_INT);
        int[] flagData = new int [WIDTH * HEIGHT];
        Arrays.fill(flagData, 1);
        Arrays.fill(flagData, 0, 3*WIDTH, 0);
        flagImage.getRaster().setDataElements(0, 0, WIDTH, HEIGHT, flagData);
        flagBand.setSourceImage(flagImage);
        product1Copy.getBand("b1").setValidPixelExpression("flag == 1");

        final MosaicOp op = new MosaicOp();
        op.setSourceProducts(new Product[]{product1Copy, product2, product3});
        op.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1")
        };
        op.conditions = new MosaicOp.Condition[]{
                new MosaicOp.Condition("b1_cond", "b1 != 3", true)
        };
        op.bounds = new MosaicOp.GeoBounds(-10, 10, 10, -10);
        op.pixelSizeX = 1;
        op.pixelSizeY = 1;

        final Product product = op.getTargetProduct();

        final GeoPos[] geoPositions = {
                new GeoPos(8, -8), new GeoPos(4, -4), new GeoPos(-1, 1), new GeoPos(-4, 4), new GeoPos(-8, 8)
        };

        Band b1Band = product.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 5.0f, 5.0f, 3.5f, 2.0f});

        Band countBand = product.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 1, 1, 2, 1});

        Band condBand = product.getBand("b1_cond");
        assertSampleValuesInt(condBand, geoPositions, new int[]{0, 1, 1, 2, 1});
    }

    @Test
    public void testMosaickingUpdate() throws IOException {
        final MosaicOp mosaicOp = new MosaicOp();
        mosaicOp.setSourceProducts(new Product[]{product1, product2});
        mosaicOp.variables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1"),
        };
        mosaicOp.conditions = new MosaicOp.Condition[]{
                new MosaicOp.Condition("b1_cond", "b1 != 3", true)
        };

        mosaicOp.bounds = new MosaicOp.GeoBounds(-10, 10, 10, -10);
        mosaicOp.pixelSizeX = 1;
        mosaicOp.pixelSizeY = 1;

        final Product mosaicProduct = mosaicOp.getTargetProduct();

        Band b1Band;
        Band countBand;
        Band condBand;

        final GeoPos[] geoPositions = {
                new GeoPos(8, -8), new GeoPos(4, -4), new GeoPos(-1, 1), new GeoPos(-4, 4), new GeoPos(-8, 8)
        };

        b1Band = mosaicProduct.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 0.0f, 2.0f, 2.0f, 2.0f});

        countBand = mosaicProduct.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 0, 1, 1, 1});

        condBand = mosaicProduct.getBand("b1_cond");
        assertSampleValuesInt(condBand, geoPositions, new int[]{0, 0, 1, 1, 1});


        final MosaicOp mosaicUpdateOp = new MosaicOp();
        mosaicUpdateOp.setSourceProducts(new Product[]{product3});
        mosaicUpdateOp.updateProduct = mosaicOp.getTargetProduct();

        final Product product = mosaicUpdateOp.getTargetProduct();
        final MetadataElement mosaicMetadata = product.getMetadataRoot().getElement("Processing_Graph");
        assertNotNull(mosaicMetadata);

        b1Band = product.getBand("b1");
        assertSampleValuesFloat(b1Band, geoPositions, new float[]{0.0f, 5.0f, 3.5f, 3.5f, 2.0f});

        countBand = product.getBand("b1_count");
        assertSampleValuesInt(countBand, geoPositions, new int[]{0, 1, 2, 2, 1});

        condBand = product.getBand("b1_cond");
        assertSampleValuesInt(condBand, geoPositions, new int[]{0, 1, 2, 2, 1});

    }

    private void assertSampleValuesFloat(Band b1Band, GeoPos[] geoPositions, float[] expectedValues) {
        GeoCoding geoCoding = b1Band.getGeoCoding();
        final Raster b1Raster = b1Band.getSourceImage().getData();
        for (int i = 0; i < geoPositions.length; i++) {
            PixelPos pp = geoCoding.getPixelPos(geoPositions[i], null);
            final float expectedValue = expectedValues[i];
            final float actualValue = b1Raster.getSampleFloat((int) pp.x, (int) pp.y, 0);
            final String message = String.format("At <%d>:", i);
            assertEquals(message, expectedValue, actualValue, 1.0e-6);
        }
    }

    private void assertSampleValuesInt(Band b1Band, GeoPos[] geoPositions, int[] expectedValues) {
        GeoCoding geoCoding = b1Band.getGeoCoding();
        final Raster b1Raster = b1Band.getSourceImage().getData();
        for (int i = 0; i < geoPositions.length; i++) {
            PixelPos pp = geoCoding.getPixelPos(geoPositions[i], null);
            final int expectedValue = expectedValues[i];
            final int actualValue = b1Raster.getSample((int) pp.x, (int) pp.y, 0);
            final String message = String.format("At <%d>:", i);
            assertEquals(message, expectedValue, actualValue);
        }
    }

    private static Product createProduct(final String name, final int easting, final int northing,
                                         final float bandFillValue) throws FactoryException, TransformException {
        final Product product = new Product(name, "T", WIDTH, HEIGHT);
        product.addBand(createBand(bandFillValue));
        final AffineTransform transform = new AffineTransform();
        transform.translate(easting, northing);
        transform.scale(1, -1);
        transform.translate(-0.5, -0.5);
        product.setGeoCoding(
                new CrsGeoCoding(CRS.decode("EPSG:4326", true), new Rectangle(0, 0, WIDTH, HEIGHT), transform));
        return product;
    }

    private static Band createBand(float fillValue) {
        final Band band = new Band("b1", ProductData.TYPE_FLOAT32, WIDTH, HEIGHT);
        band.setSourceImage(ConstantDescriptor.create((float) WIDTH, (float) HEIGHT, new Float[]{fillValue}, null));
        return band;
    }


}