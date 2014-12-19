/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import java.io.File;
import org.geotools.data.CachingFeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;

/**
 *
 * @author me
 */
public class SwingMap {
    /**
     * This method demonstrates using a memory-based cache to speed up the display (e.g. when
     * zooming in and out).
     * 
     * There is just one line extra compared to the main method, where we create an instance of
     * CachingFeatureStore.
     */
    public static void main(String[] args) throws Exception {
        // display a data store file chooser dialog for shapefiles
        /*File file = JFileDataStoreChooser.showOpenFile("shp", null);
        if (file == null) {
            return;
        }*/
        
        File file = new File("/home/me/share/climatenet/data/kmlcvr016485035249379473443/cvr01_Point.shp");

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        
        // CachingFeatureSource is deprecated as experimental (not yet production ready)
        //CachingFeatureSource cache = new CachingFeatureSource(featureSource);

        // Create a map content and add our shapefile to it
        MapContent map = new MapContent();
        map.setTitle("Using cached features");
        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);

        // Now display the map
        JMapFrame.showMap(map);
    }    
}